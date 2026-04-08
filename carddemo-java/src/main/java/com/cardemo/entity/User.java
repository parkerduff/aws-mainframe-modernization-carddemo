package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * User entity - migrated from COBOL copybook CSUSR01Y.cpy (USRSEC VSAM file).
 * Maps the SEC-USER-DATA record layout for authentication and authorization.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", length = 8)
    @NotBlank
    @Size(max = 8)
    private String userId;

    @Column(name = "first_name", length = 20, nullable = false)
    @NotBlank
    @Size(max = 20)
    private String firstName;

    @Column(name = "last_name", length = 20, nullable = false)
    @NotBlank
    @Size(max = 20)
    private String lastName;

    @Column(name = "password", nullable = false)
    @NotBlank
    private String password;

    @Column(name = "user_type", length = 1, nullable = false)
    @Size(max = 1)
    private String userType = "U";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {}

    public User(String userId, String firstName, String lastName, String password, String userType) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.userType = userType;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isAdmin() {
        return "A".equals(userType);
    }
}
