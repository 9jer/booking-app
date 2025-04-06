package com.example.userservice.services;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.util.ErrorsUtil;
import com.example.userservice.util.UserException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public User createNewUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(List.of(roleService.getGuestRole()));
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()->
                new UserException(String.format("User %s not found", id)));
    }

    @Override
    @Transactional
    public User updateUserById(Long id, SaveUserDTO updatedUser) {

        ErrorsUtil.validateInputUserData(updatedUser, userRepository.findByUsername(updatedUser.getUsername())
                ,userRepository.findByEmail(updatedUser.getEmail())
        );

        User exsitingUser = getUserById(id);

        User user = convertUserDTOToUser(updatedUser);
        enrichPropertyForUpdate(exsitingUser, user);
        
        return userRepository.save(exsitingUser);
    }


    private User convertUserDTOToUser(SaveUserDTO saveUserDTO){
        return modelMapper.map(saveUserDTO, User.class);
    }

    private void enrichPropertyForUpdate(User exsitingUser, User updatedUser) {
        exsitingUser.setUsername(updatedUser.getUsername());
        exsitingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        exsitingUser.setName(updatedUser.getName());
        exsitingUser.setEmail(updatedUser.getEmail());
        exsitingUser.setPhone(updatedUser.getPhone());
        exsitingUser.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    public User assignOwnerRole(Long userId) {
        User user = getUserById(userId);
        Role ownerRole = roleService.getOwnerRole();
        user.getRoles().add(ownerRole);
        return userRepository.save(user);
    }

    @Override
    public Boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }
}
