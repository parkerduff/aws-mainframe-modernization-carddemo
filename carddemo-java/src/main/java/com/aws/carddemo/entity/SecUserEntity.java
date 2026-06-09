package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Security user entity, migrated from COBOL copybook {@code app/cpy/CSUSR01Y.cpy} (SEC-USER-DATA).
 *
 * <pre>
 *   05 SEC-USR-ID    PIC X(08).  -> usrId (PK)
 *   05 SEC-USR-FNAME PIC X(20).  -> usrFname
 *   05 SEC-USR-LNAME PIC X(20).  -> usrLname
 *   05 SEC-USR-PWD   PIC X(08).  -> usrPwd (now a BCrypt hash, not plaintext)
 *   05 SEC-USR-TYPE  PIC X(01).  -> usrType ('A' admin, 'U' user)
 * </pre>
 *
 * The legacy file stored an 8-character plaintext password; the migration stores a
 * BCrypt hash instead, so the column width is widened accordingly.
 */
@Entity
@Table(name = "sec_user")
@Getter
@Setter
@NoArgsConstructor
public class SecUserEntity {

    @Id
    @Column(name = "usr_id", length = 8, nullable = false)
    private String usrId;

    @Column(name = "usr_fname", length = 20)
    private String usrFname;

    @Column(name = "usr_lname", length = 20)
    private String usrLname;

    @Column(name = "usr_pwd", length = 100)
    private String usrPwd;

    @Column(name = "usr_type", length = 1)
    private String usrType;
}
