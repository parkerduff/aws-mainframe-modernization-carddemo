package com.cardemo.service;

import com.cardemo.dto.request.UserCreateRequest;
import com.cardemo.dto.request.UserUpdateRequest;
import com.cardemo.entity.User;
import com.cardemo.exception.BusinessException;
import com.cardemo.exception.ResourceNotFoundException;
import com.cardemo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Admin service - migrated from COBOL programs:
 * - COUSR00C.cbl (CU00 transaction) - user list
 * - COUSR01C.cbl (CU01 transaction) - add user
 * - COUSR02C.cbl (CU02 transaction) - update user
 * - COUSR03C.cbl (CU03 transaction) - delete user
 * Replaces VSAM operations on USRSEC KSDS file.
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Transactional
    public User createUser(UserCreateRequest request) {
        if (userRepository.findByUserId(request.userId()).isPresent()) {
            throw new BusinessException("User already exists: " + request.userId());
        }

        User user = new User();
        user.setUserId(request.userId().toUpperCase());
        user.setFirstName(request.firstName().toUpperCase());
        user.setLastName(request.lastName().toUpperCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setUserType(request.userType() != null ? request.userType() : "U");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("User created: {} (type: {})", saved.getUserId(), saved.getUserType());
        return saved;
    }

    @Transactional
    public User updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName().toUpperCase());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName().toUpperCase());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.userType() != null) {
            user.setUserType(request.userType());
        }
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("User updated: {}", userId);
        return saved;
    }

    @Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userRepository.delete(user);
        log.info("User deleted: {}", userId);
    }
}
