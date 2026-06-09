package com.aws.carddemo.service;

import com.aws.carddemo.domain.UserType;
import com.aws.carddemo.dto.MenuOption;
import com.aws.carddemo.dto.MenuResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Menu service, replacing {@code app/cbl/COMEN01C.cbl} (regular-user main menu) and
 * {@code app/cbl/COADM01C.cbl} (admin main menu). The options returned depend on the
 * authenticated user's type, mirroring the COBOL CDEMO-USRTYP-ADMIN / CDEMO-USRTYP-USER
 * routing.
 */
@Service
public class MenuService {

    private static final List<MenuOption> USER_OPTIONS = List.of(
            new MenuOption(1, "Account View", "COACTVWC"),
            new MenuOption(2, "Account Update", "COACTUPC"),
            new MenuOption(3, "Card List", "COCRDLIC"),
            new MenuOption(4, "Card Detail", "COCRDSLC"),
            new MenuOption(5, "Card Update", "COCRDUPC"),
            new MenuOption(6, "Transaction List", "COTRN00C"),
            new MenuOption(7, "Transaction View", "COTRN01C"),
            new MenuOption(8, "Transaction Add", "COTRN02C"),
            new MenuOption(9, "Transaction Reports", "CORPT00C"),
            new MenuOption(10, "Bill Payment", "COBIL00C"));

    private static final List<MenuOption> ADMIN_OPTIONS = List.of(
            new MenuOption(1, "User List (Security)", "COUSR00C"),
            new MenuOption(2, "User Add (Security)", "COUSR01C"),
            new MenuOption(3, "User Update (Security)", "COUSR02C"),
            new MenuOption(4, "User Delete (Security)", "COUSR03C"));

    public MenuResponse getMenu(String userTypeCode) {
        UserType userType = UserType.fromCode(userTypeCode);
        if (userType == UserType.ADMIN) {
            return new MenuResponse(userType.getCodeString(), "Admin Menu", ADMIN_OPTIONS);
        }
        return new MenuResponse(userType.getCodeString(), "Main Menu", USER_OPTIONS);
    }
}
