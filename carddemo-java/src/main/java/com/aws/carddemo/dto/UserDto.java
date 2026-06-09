package com.aws.carddemo.dto;

import com.aws.carddemo.entity.SecUserEntity;

/**
 * User response, mirroring the user records managed by COUSR00C-COUSR03C. The password
 * hash is never exposed.
 */
public record UserDto(
        String userId,
        String firstName,
        String lastName,
        String userType) {

    public static UserDto fromEntity(SecUserEntity e) {
        return new UserDto(
                e.getUsrId(),
                e.getUsrFname(),
                e.getUsrLname(),
                e.getUsrType());
    }
}
