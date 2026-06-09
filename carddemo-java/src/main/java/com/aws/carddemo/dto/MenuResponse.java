package com.aws.carddemo.dto;

import java.util.List;

/**
 * Menu response, replacing COMEN01C (regular user menu) and COADM01C (admin menu). The
 * set of options returned depends on the authenticated user's type.
 */
public record MenuResponse(
        String userType,
        String menuTitle,
        List<MenuOption> options) {
}
