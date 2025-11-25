package com.example.userservice.services;

import com.example.userservice.dto.SaveUserDTO;
import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.util.ErrorsUtil;
import com.example.userservice.util.JwtTokenUtils;
import com.example.userservice.util.UserException;
import org.modelmapper.ModelMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service("userServiceImpl")
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final CacheManager cacheManager;
    private final JwtTokenUtils jwtTokenUtils;

    private final UserService self;

    public UserServiceImpl(@Lazy UserService self, UserRepository userRepository, RoleService roleService,
                           PasswordEncoder passwordEncoder, ModelMapper modelMapper, CacheManager cacheManager,
                           JwtTokenUtils jwtTokenUtils) {
        this.self = self;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.cacheManager = cacheManager;
        this.jwtTokenUtils = jwtTokenUtils;
    }

    @Override
    @Cacheable(value = "userByUsername", key = "#username", unless = "#result == null")
    public Optional<User> findByUsername(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        return userOptional.map(this::convertToDetachedUser);
    }

    @Override
    @Cacheable(value = "userByEmail", key = "#email", unless = "#result == null")
    public Optional<User> findByEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        return userOptional.map(this::convertToDetachedUser);
    }

    @Override
    @Transactional
    @Caching(
            put = {
                    @CachePut(value = "userById", key = "#result.id"),
                    @CachePut(value = "userByUsername", key = "#result.username")
            },
            evict = {
                    @CacheEvict(value = "userByEmail", key = "#result.email")
            })
    public User createNewUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(List.of(roleService.getGuestRole()));
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        return convertToDetachedUser(savedUser);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Cacheable(value = "userById", key = "#id")
    public User getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new UserException(String.format("User %s not found", id)));
        return convertToDetachedUser(user);
    }

    @Override
    @Transactional
    @CachePut(value = "userById", key = "#id")
    public User updateUserById(Long id, SaveUserDTO updatedUser, String token) {
        Long currentUserId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        if (!roles.contains("ROLE_ADMIN") && !currentUserId.equals(id)) {
            throw new UserException("You can only update your own account.");
        }

        Optional<User> userByUsername = self.findByUsername(updatedUser.getUsername());
        Optional<User> userByEmail = self.findByEmail(updatedUser.getEmail());

        if(userByUsername.isPresent() && userByUsername.get().getId().equals(id)) {
            userByUsername = Optional.empty();
        }

        if(userByEmail.isPresent() && userByEmail.get().getId().equals(id)) {
            userByEmail = Optional.empty();
        }

        ErrorsUtil.validateInputUserData(updatedUser, userByUsername, userByEmail);

        User existingUser = userRepository.findById(id).orElseThrow(() ->
                new UserException(String.format("User %s not found", id)));

        evictUserCaches(existingUser);

        User user = convertUserDTOToUser(updatedUser);
        enrichPropertyForUpdate(existingUser, user);

        User savedUser = userRepository.save(existingUser);
        return convertToDetachedUser(savedUser);
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
    @CachePut(value = "userById", key = "#userId")
    public User assignOwnerRole(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserException("User not found"));

        Role ownerRole = roleService.getOwnerRole();

        if (user.getRoles().stream().anyMatch(role -> role.getName().equals(ownerRole.getName()))) {
            throw new UserException("User already has OWNER role");
        }

        evictUserCaches(user);
        user.getRoles().add(ownerRole);

        User savedUser = userRepository.save(user);
        return convertToDetachedUser(savedUser);
    }

    @Override
    public Boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteUserById(Long id, String token) {
        Long currentUserId = jwtTokenUtils.getUserId(token);
        List<String> roles = jwtTokenUtils.getRoles(token);

        if (!roles.contains("ROLE_ADMIN") && !currentUserId.equals(id)) {
            throw new UserException("You can only delete your own account.");
        }

        User user = userRepository.findById(id).orElseThrow(() -> new UserException("User not found"));

        evictUserCaches(user);
        if (cacheManager.getCache("userById") != null) {
            Objects.requireNonNull(cacheManager.getCache("userById")).evict(id);
        }

        userRepository.delete(user);
    }

    private void evictUserCaches(User user) {
        if (user.getUsername() != null) {
            Cache usernameCache = cacheManager.getCache("userByUsername");
            if (usernameCache != null) {
                usernameCache.evict(user.getUsername());
            }
        }
        if (user.getEmail() != null) {
            Cache emailCache = cacheManager.getCache("userByEmail");
            if (emailCache != null) {
                emailCache.evict(user.getEmail());
            }
        }
    }

    private User convertToDetachedUser(User user) {
        User detachedUser = new User();
        detachedUser.setId(user.getId());
        detachedUser.setUsername(user.getUsername());
        detachedUser.setEmail(user.getEmail());
        detachedUser.setName(user.getName());
        detachedUser.setPhone(user.getPhone());
        detachedUser.setPassword(user.getPassword());
        detachedUser.setCreatedAt(user.getCreatedAt());
        detachedUser.setUpdatedAt(user.getUpdatedAt());

        if (user.getRoles() != null) {
            detachedUser.setRoles(new ArrayList<>(user.getRoles()));
        } else {
            detachedUser.setRoles(new ArrayList<>());
        }

        return detachedUser;
    }
}
