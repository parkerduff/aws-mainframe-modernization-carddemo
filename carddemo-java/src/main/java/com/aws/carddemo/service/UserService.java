package com.aws.carddemo.service;

import com.aws.carddemo.domain.UserType;
import com.aws.carddemo.dto.UserCreateDto;
import com.aws.carddemo.dto.UserDto;
import com.aws.carddemo.dto.UserUpdateDto;
import com.aws.carddemo.entity.SecUserEntity;
import com.aws.carddemo.exception.BusinessRuleException;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.repository.SecUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * User maintenance service, replacing {@code app/cbl/COUSR00C.cbl} (list),
 * {@code COUSR01C.cbl} (add), {@code COUSR02C.cbl} (update) and {@code COUSR03C.cbl} (delete).
 *
 * <p>These are the admin-only user-security programs reached from the admin menu (COADM01C).
 * Passwords were 8-char plaintext (SEC-USR-PWD) in the legacy USRSEC file; here they are
 * BCrypt-hashed before storage.</p>
 */
@Service
public class UserService {

    private final SecUserRepository secUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(SecUserRepository secUserRepository, PasswordEncoder passwordEncoder) {
        this.secUserRepository = secUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Pageable pageable) {
        return secUserRepository.findAll(pageable).map(UserDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(String userId) {
        return secUserRepository.findById(normalizeId(userId))
                .map(UserDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Transactional
    public UserDto createUser(UserCreateDto dto) {
        String userId = normalizeId(dto.userId());
        if (secUserRepository.existsById(userId)) {
            throw new BusinessRuleException("User ID already exist...");
        }
        SecUserEntity user = new SecUserEntity();
        user.setUsrId(userId);
        user.setUsrFname(dto.firstName());
        user.setUsrLname(dto.lastName());
        user.setUsrPwd(passwordEncoder.encode(dto.password()));
        user.setUsrType(UserType.fromCode(dto.userType()).getCodeString());
        return UserDto.fromEntity(secUserRepository.save(user));
    }

    @Transactional
    public UserDto updateUser(String userId, UserUpdateDto dto) {
        SecUserEntity user = secUserRepository.findById(normalizeId(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (dto.firstName() != null) {
            user.setUsrFname(dto.firstName());
        }
        if (dto.lastName() != null) {
            user.setUsrLname(dto.lastName());
        }
        if (StringUtils.hasText(dto.password())) {
            user.setUsrPwd(passwordEncoder.encode(dto.password()));
        }
        if (dto.userType() != null) {
            user.setUsrType(UserType.fromCode(dto.userType()).getCodeString());
        }
        return UserDto.fromEntity(secUserRepository.save(user));
    }

    @Transactional
    public void deleteUser(String userId) {
        String normalized = normalizeId(userId);
        if (!secUserRepository.existsById(normalized)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        secUserRepository.deleteById(normalized);
    }

    private String normalizeId(String userId) {
        return userId == null ? null : userId.trim().toUpperCase();
    }
}
