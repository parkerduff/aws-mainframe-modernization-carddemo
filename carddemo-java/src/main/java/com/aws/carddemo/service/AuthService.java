package com.aws.carddemo.service;

import com.aws.carddemo.domain.UserType;
import com.aws.carddemo.dto.LoginRequest;
import com.aws.carddemo.dto.LoginResponse;
import com.aws.carddemo.entity.SecUserEntity;
import com.aws.carddemo.exception.AuthenticationFailedException;
import com.aws.carddemo.repository.SecUserRepository;
import com.aws.carddemo.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Sign-on service, replacing {@code app/cbl/COSGN00C.cbl}.
 *
 * <p>The legacy READ-USER-SEC-FILE paragraph reads the USRSEC file by user id, compares the
 * password, and on success XCTLs to COADM01C (admins) or COMEN01C (regular users); on
 * failure it sets WS-ERR-FLG with "User not found" (RESP=13) or "Wrong Password". This
 * service reproduces that logic: look up the user, verify the BCrypt-hashed password, and
 * return a JWT plus the next-program routing hint.</p>
 */
@Service
public class AuthService {

    private static final String PROGRAM_NAME = "COSGN00C";
    private static final String ADMIN_MENU_PROGRAM = "COADM01C";
    private static final String USER_MENU_PROGRAM = "COMEN01C";

    private final SecUserRepository secUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(SecUserRepository secUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.secUserRepository = secUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        // COBOL: MOVE FUNCTION UPPER-CASE(USERIDI) TO WS-USER-ID
        String userId = request.userId().trim().toUpperCase();

        // COBOL: EXEC CICS READ DATASET(USRSEC) ... WHEN 13 -> "User not found"
        SecUserEntity user = secUserRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException(
                        "User not found. Try again ..."));

        // COBOL: IF SEC-USR-PWD = WS-USER-PWD ... ELSE "Wrong Password. Try again ..."
        if (!passwordEncoder.matches(request.password(), user.getUsrPwd())) {
            throw new AuthenticationFailedException("Wrong Password. Try again ...");
        }

        UserType userType = UserType.fromCode(user.getUsrType());
        String token = tokenProvider.generateToken(
                user.getUsrId(), userType.getCodeString(), PROGRAM_NAME);

        // COBOL: IF CDEMO-USRTYP-ADMIN XCTL COADM01C ELSE XCTL COMEN01C
        String nextProgram = userType == UserType.ADMIN ? ADMIN_MENU_PROGRAM : USER_MENU_PROGRAM;

        return new LoginResponse(token, user.getUsrId(), userType.getCodeString(), nextProgram);
    }
}
