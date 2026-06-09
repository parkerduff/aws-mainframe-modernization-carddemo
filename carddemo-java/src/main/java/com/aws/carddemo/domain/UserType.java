package com.aws.carddemo.domain;

/**
 * User type, migrated from the COBOL 88-level conditions in COCOM01Y.cpy:
 * <pre>
 *   88 CDEMO-USRTYP-ADMIN VALUE 'A'.
 *   88 CDEMO-USRTYP-USER  VALUE 'U'.
 * </pre>
 * The single-character code ('A'/'U') is the value persisted in the {@code sec_user.usr_type}
 * column, mirroring the legacy {@code SEC-USR-TYPE PIC X(01)} field.
 */
public enum UserType {

    ADMIN('A'),
    USER('U');

    private final char code;

    UserType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public String getCodeString() {
        return String.valueOf(code);
    }

    /**
     * Resolve a {@link UserType} from its single-character COBOL code.
     *
     * @param code 'A' for admin, 'U' (or anything else) for a regular user
     * @return the matching user type
     */
    public static UserType fromCode(String code) {
        if (code != null && !code.isBlank() && Character.toUpperCase(code.charAt(0)) == 'A') {
            return ADMIN;
        }
        return USER;
    }

    /**
     * @return the Spring Security role name (e.g. {@code ROLE_ADMIN}).
     */
    public String roleName() {
        return "ROLE_" + name();
    }
}
