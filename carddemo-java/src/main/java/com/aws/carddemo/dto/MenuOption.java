package com.aws.carddemo.dto;

/**
 * A single menu option, mirroring an entry on the COMEN01C (user) or COADM01C (admin) menu.
 */
public record MenuOption(
        int optionNumber,
        String name,
        String targetProgram) {
}
