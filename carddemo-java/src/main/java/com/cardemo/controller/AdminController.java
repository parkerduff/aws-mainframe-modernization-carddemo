package com.cardemo.controller;

import com.cardemo.dto.request.UserCreateRequest;
import com.cardemo.dto.request.UserUpdateRequest;
import com.cardemo.entity.User;
import com.cardemo.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller - migrated from COBOL programs:
 * - COUSR00C.cbl (CU00 transaction) - user list
 * - COUSR01C.cbl (CU01 transaction) - add user
 * - COUSR02C.cbl (CU02 transaction) - update user
 * - COUSR03C.cbl (CU03 transaction) - delete user
 * Replaces CICS admin screens COUSR00-03 (BMS maps).
 * Requires ADMIN role (like COBOL USRTYPE='A' check).
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administration", description = "User administration (migrated from COUSR00C-03C)")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @Operation(summary = "List users", description = "Paginated user list (replaces CICS CU00 transaction)")
    public ResponseEntity<Page<User>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(adminService.listUsers(pageable));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "View user", description = "User details")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.getUser(userId));
    }

    @PostMapping("/users")
    @Operation(summary = "Create user", description = "Add new user (replaces CICS CU01 transaction)")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(adminService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "Update user", description = "Update user (replaces CICS CU02 transaction)")
    public ResponseEntity<User> updateUser(@PathVariable String userId,
                                           @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateUser(userId, request));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user", description = "Remove user (replaces CICS CU03 transaction)")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
