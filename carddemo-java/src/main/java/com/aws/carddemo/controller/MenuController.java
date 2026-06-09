package com.aws.carddemo.controller;

import com.aws.carddemo.domain.UserType;
import com.aws.carddemo.dto.MenuResponse;
import com.aws.carddemo.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Menu endpoint, replacing {@code COMEN01C} (user menu) and {@code COADM01C} (admin menu).
 * The returned options depend on the authenticated user's type, derived from their granted
 * authority (ROLE_ADMIN -&gt; admin menu, otherwise user menu).
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<MenuResponse> getMenu(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(UserType.ADMIN.roleName()::equals);
        String userTypeCode = isAdmin ? UserType.ADMIN.getCodeString() : UserType.USER.getCodeString();
        return ResponseEntity.ok(menuService.getMenu(userTypeCode));
    }
}
