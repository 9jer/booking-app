package com.example.userservice.services;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.util.JwtTokenUtils;
import com.example.userservice.util.UserException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("userServiceImpl")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final JwtTokenUtils jwtTokenUtils;

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public UserDTO createNewUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(List.of(roleService.getGuestRole()));
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        return convertToUserDTO(savedUser);
    }

    @Override
    public List<UserDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "userById", key = "#id")
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new UserException(String.format("User %s not found", id)));
        return convertToUserDTO(user);
    }

    @Override
    @Transactional
    @CachePut(value = "userById", key = "#id")
    public UserDTO updateUserById(Long id, SaveUserDTO updatedUser, String token) {
        Long currentUserId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        if (!roles.contains("ROLE_ADMIN") && !currentUserId.equals(id)) {
            throw new UserException("You can only update your own account.");
        }

        if (!updatedUser.getPassword().equals(updatedUser.getConfirmPassword())) {
            throw new UserException("Incorrect password!");
        }

        Optional<User> userByUsername = userRepository.findByUsername(updatedUser.getUsername());
        Optional<User> userByEmail = userRepository.findByEmail(updatedUser.getEmail());

        if(userByUsername.isPresent() && !userByUsername.get().getId().equals(id)) {
            throw new UserException("Username already taken");
        }
        if(userByEmail.isPresent() && !userByEmail.get().getId().equals(id)) {
            throw new UserException("Email already taken");
        }

        User existingUser = userRepository.findById(id).orElseThrow(() ->
                new UserException(String.format("User %s not found", id)));

        User user = convertSaveUserDTOToUser(updatedUser);
        enrichPropertyForUpdate(existingUser, user);

        User savedUser = userRepository.save(existingUser);
        return convertToUserDTO(savedUser);
    }

    @Override
    @Transactional
    @CachePut(value = "userById", key = "#userId")
    public UserDTO assignOwnerRole(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserException("User not found"));

        Role ownerRole = roleService.getOwnerRole();

        if (user.getRoles().stream().anyMatch(role -> role.getName().equals(ownerRole.getName()))) {
            throw new UserException("User already has OWNER role");
        }

        user.getRoles().add(ownerRole);
        User savedUser = userRepository.save(user);

        return convertToUserDTO(savedUser);
    }

    @Override
    public Boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userById", key = "#id")
    public void deleteUserById(Long id, String token) {
        Long currentUserId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        if (!roles.contains("ROLE_ADMIN") && !currentUserId.equals(id)) {
            throw new UserException("You can only delete your own account.");
        }

        if (!userRepository.existsById(id)) {
            throw new UserException("User not found");
        }

        userRepository.deleteById(id);
    }

    private UserDTO convertToUserDTO(User user) {
        UserDTO dto = modelMapper.map(user, UserDTO.class);
        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private User convertSaveUserDTOToUser(SaveUserDTO saveUserDTO){
        return modelMapper.map(saveUserDTO, User.class);
    }

    private void enrichPropertyForUpdate(User existingUser, User updatedUser) {
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setUpdatedAt(LocalDateTime.now());
    }
}
