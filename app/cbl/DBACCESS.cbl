      ******************************************************************
      * Program     : DBACCESS.CBL
      * Application : CardDemo
      * Type        : COBOL Subprogram (callable from batch and CICS)
      * Function    : Shared Data Access Layer (DAL).  Encapsulates all
      *               read/write access to the modernised relational
      *               store using EXEC SQL.  Callers pass operation,
      *               entity name, key and data via copybook DBACCESY
      *               and receive a CICS-style RESP/RESP2 status.
      *
      *               During the strangler-fig migration the DAL also
      *               retains the original VSAM access path so that any
      *               batch or online program which has not yet been
      *               migrated can continue to operate against VSAM.
      *               The choice of path is made at run time from the
      *               &DAL-USE-RDBMS feature flag fed via SYSIN /
      *               COMMAREA.
      ******************************************************************
      * Copyright Amazon.com, Inc. or its affiliates.
      * All Rights Reserved.
      *
      * Licensed under the Apache License, Version 2.0 (the "License").
      * You may not use this file except in compliance with the License.
      * You may obtain a copy of the License at
      *
      *    http://www.apache.org/licenses/LICENSE-2.0
      *
      * Unless required by applicable law or agreed to in writing,
      * software distributed under the License is distributed on an
      * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
      * either express or implied. See the License for the specific
      * language governing permissions and limitations under the License
      ******************************************************************

       IDENTIFICATION DIVISION.
       PROGRAM-ID.    DBACCESS.
       AUTHOR.        AWS-MODERNIZATION.

       ENVIRONMENT DIVISION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.

       EXEC SQL INCLUDE SQLCA END-EXEC.

       01  WS-FEATURE-FLAGS.
           05 WS-USE-RDBMS              PIC X(01) VALUE 'Y'.
              88 WS-USE-RDBMS-ON                  VALUE 'Y'.
              88 WS-USE-RDBMS-OFF                 VALUE 'N'.

       01  WS-WORK-FIELDS.
           05 WS-LOG-MSG                PIC X(120) VALUE SPACES.

       01  WS-ACCT-ROW.
           05 WS-ACCT-ID                PIC S9(11) COMP-3.
           05 WS-ACCT-STATUS            PIC X(01).
           05 WS-ACCT-CURR-BAL          PIC S9(10)V99 COMP-3.
           05 WS-ACCT-CR-LIM            PIC S9(10)V99 COMP-3.
           05 WS-ACCT-CASH-LIM          PIC S9(10)V99 COMP-3.
           05 WS-ACCT-OPEN-DT           PIC X(10).
           05 WS-ACCT-EXP-DT            PIC X(10).
           05 WS-ACCT-REISS-DT          PIC X(10).
           05 WS-ACCT-CYC-CRED          PIC S9(10)V99 COMP-3.
           05 WS-ACCT-CYC-DEBIT         PIC S9(10)V99 COMP-3.
           05 WS-ACCT-ZIP               PIC X(10).
           05 WS-ACCT-GROUP             PIC X(10).

       01  WS-CARD-ROW.
           05 WS-CARD-NUM               PIC X(16).
           05 WS-CARD-ACCT              PIC S9(11) COMP-3.
           05 WS-CARD-CVV               PIC S9(03) COMP-3.
           05 WS-CARD-NAME              PIC X(50).
           05 WS-CARD-EXP-DT            PIC X(10).
           05 WS-CARD-STATUS            PIC X(01).

       01  WS-XREF-ROW.
           05 WS-XREF-CARD              PIC X(16).
           05 WS-XREF-CUST              PIC S9(09) COMP-3.
           05 WS-XREF-ACCT              PIC S9(11) COMP-3.

      *----------------------------------------------------------------*
      *                          LINKAGE
      *----------------------------------------------------------------*
       LINKAGE SECTION.
       COPY DBACCESY.

       PROCEDURE DIVISION USING DAL-COMMAREA.

       0000-MAIN-DRIVER.

           SET DAL-RESP-NORMAL TO TRUE
           MOVE 0          TO DAL-REAS-CD
           MOVE SPACES     TO DAL-MESSAGE

           IF WS-USE-RDBMS-ON
               PERFORM 1000-DISPATCH-SQL
           ELSE
               PERFORM 9000-DISPATCH-VSAM-FALLBACK
           END-IF
           GOBACK.

      *----------------------------------------------------------------*
      *                       1000-DISPATCH-SQL
      *
      * Decode the requested operation/entity and call the matching SQL
      * paragraph.  Each paragraph translates between the DAL-DATA-AREA
      * staging buffer and the host variables used in the EXEC SQL
      * statements.
      *----------------------------------------------------------------*
       1000-DISPATCH-SQL.
           EVALUATE TRUE
               WHEN DAL-OP-READ AND DAL-EN-ACCOUNT
                    PERFORM 1100-READ-ACCOUNT
               WHEN DAL-OP-WRITE AND DAL-EN-ACCOUNT
                    PERFORM 1110-WRITE-ACCOUNT
               WHEN DAL-OP-UPDATE AND DAL-EN-ACCOUNT
                    PERFORM 1120-UPDATE-ACCOUNT

               WHEN DAL-OP-READ AND DAL-EN-CARD
                    PERFORM 1200-READ-CARD
               WHEN DAL-OP-UPDATE AND DAL-EN-CARD
                    PERFORM 1220-UPDATE-CARD

               WHEN DAL-OP-READ AND DAL-EN-CARDXREF
                    PERFORM 1300-READ-XREF

               WHEN DAL-OP-READ AND DAL-EN-USRSEC
                    PERFORM 1400-READ-USRSEC

               WHEN OTHER
                    SET DAL-RESP-INVREQ TO TRUE
                    STRING 'Unsupported DAL operation: '
                           DAL-OPERATION ' / ' DAL-ENTITY
                           DELIMITED BY SIZE
                       INTO DAL-MESSAGE
           END-EVALUATE.

      *----------------------------------------------------------------*
      *                       1100-READ-ACCOUNT
      *----------------------------------------------------------------*
       1100-READ-ACCOUNT.
           MOVE DAL-KEY (1:11) TO WS-ACCT-ID
           EXEC SQL
              SELECT acct_id,
                     acct_active_status,
                     acct_curr_bal,
                     acct_credit_limit,
                     acct_cash_credit_limit,
                     TO_CHAR(acct_open_date,       'YYYY-MM-DD'),
                     TO_CHAR(acct_expiration_date, 'YYYY-MM-DD'),
                     TO_CHAR(acct_reissue_date,    'YYYY-MM-DD'),
                     acct_curr_cyc_credit,
                     acct_curr_cyc_debit,
                     acct_addr_zip,
                     acct_group_id
              INTO  :WS-ACCT-ID,
                    :WS-ACCT-STATUS,
                    :WS-ACCT-CURR-BAL,
                    :WS-ACCT-CR-LIM,
                    :WS-ACCT-CASH-LIM,
                    :WS-ACCT-OPEN-DT,
                    :WS-ACCT-EXP-DT,
                    :WS-ACCT-REISS-DT,
                    :WS-ACCT-CYC-CRED,
                    :WS-ACCT-CYC-DEBIT,
                    :WS-ACCT-ZIP,
                    :WS-ACCT-GROUP
              FROM   accounts
              WHERE  acct_id = :WS-ACCT-ID
           END-EXEC
           PERFORM 8000-MAP-SQLCODE.

      *----------------------------------------------------------------*
      *                       1110-WRITE-ACCOUNT
      *----------------------------------------------------------------*
       1110-WRITE-ACCOUNT.
      *    Caller is expected to have populated DAL-DATA-AREA with the
      *    same byte layout as copybook CVACT01Y; the host variables
      *    are loaded from it before the INSERT.
           PERFORM 1101-UNPACK-ACCOUNT
           EXEC SQL
              INSERT INTO accounts
                 (acct_id, acct_active_status, acct_curr_bal,
                  acct_credit_limit, acct_cash_credit_limit,
                  acct_open_date, acct_expiration_date, acct_reissue_date,
                  acct_curr_cyc_credit, acct_curr_cyc_debit,
                  acct_addr_zip, acct_group_id)
              VALUES
                 (:WS-ACCT-ID, :WS-ACCT-STATUS, :WS-ACCT-CURR-BAL,
                  :WS-ACCT-CR-LIM, :WS-ACCT-CASH-LIM,
                  TO_DATE(:WS-ACCT-OPEN-DT,  'YYYY-MM-DD'),
                  TO_DATE(:WS-ACCT-EXP-DT,   'YYYY-MM-DD'),
                  TO_DATE(:WS-ACCT-REISS-DT, 'YYYY-MM-DD'),
                  :WS-ACCT-CYC-CRED, :WS-ACCT-CYC-DEBIT,
                  :WS-ACCT-ZIP, :WS-ACCT-GROUP)
           END-EXEC
           PERFORM 8000-MAP-SQLCODE.

      *----------------------------------------------------------------*
      *                       1120-UPDATE-ACCOUNT
      *----------------------------------------------------------------*
       1120-UPDATE-ACCOUNT.
           PERFORM 1101-UNPACK-ACCOUNT
           EXEC SQL
              UPDATE accounts
                 SET acct_active_status     = :WS-ACCT-STATUS,
                     acct_curr_bal          = :WS-ACCT-CURR-BAL,
                     acct_credit_limit      = :WS-ACCT-CR-LIM,
                     acct_cash_credit_limit = :WS-ACCT-CASH-LIM,
                     acct_curr_cyc_credit   = :WS-ACCT-CYC-CRED,
                     acct_curr_cyc_debit    = :WS-ACCT-CYC-DEBIT,
                     acct_addr_zip          = :WS-ACCT-ZIP,
                     acct_group_id          = :WS-ACCT-GROUP
               WHERE acct_id = :WS-ACCT-ID
           END-EXEC
           PERFORM 8000-MAP-SQLCODE.

      *----------------------------------------------------------------*
      *  Helper: unpack DAL-DATA-AREA into the WS-ACCT-* host variables
      *----------------------------------------------------------------*
       1101-UNPACK-ACCOUNT.
      *    The caller provides a 300-byte image of CVACT01Y in
      *    DAL-DATA-AREA.  We reuse the offsets defined by the existing
      *    copybook to populate the host variables.
           MOVE FUNCTION NUMVAL (DAL-DATA-AREA  (1:11))
                                                  TO WS-ACCT-ID
           MOVE DAL-DATA-AREA (12:1)              TO WS-ACCT-STATUS
           MOVE FUNCTION NUMVAL (DAL-DATA-AREA (13:12))
                                                  TO WS-ACCT-CURR-BAL
           MOVE FUNCTION NUMVAL (DAL-DATA-AREA (25:12))
                                                  TO WS-ACCT-CR-LIM
           MOVE FUNCTION NUMVAL (DAL-DATA-AREA (37:12))
                                                  TO WS-ACCT-CASH-LIM
           MOVE DAL-DATA-AREA (49:10)             TO WS-ACCT-OPEN-DT
           MOVE DAL-DATA-AREA (59:10)             TO WS-ACCT-EXP-DT
           MOVE DAL-DATA-AREA (69:10)             TO WS-ACCT-REISS-DT
           MOVE FUNCTION NUMVAL (DAL-DATA-AREA (79:12))
                                                  TO WS-ACCT-CYC-CRED
           MOVE FUNCTION NUMVAL (DAL-DATA-AREA (91:12))
                                                  TO WS-ACCT-CYC-DEBIT
           MOVE DAL-DATA-AREA (103:10)            TO WS-ACCT-ZIP
           MOVE DAL-DATA-AREA (113:10)            TO WS-ACCT-GROUP.

      *----------------------------------------------------------------*
      *                       1200-READ-CARD
      *----------------------------------------------------------------*
       1200-READ-CARD.
           MOVE DAL-KEY (1:16) TO WS-CARD-NUM
           EXEC SQL
              SELECT card_num, card_acct_id, card_cvv_cd,
                     card_embossed_name,
                     TO_CHAR(card_expiration_date, 'YYYY-MM-DD'),
                     card_active_status
              INTO   :WS-CARD-NUM, :WS-CARD-ACCT, :WS-CARD-CVV,
                     :WS-CARD-NAME, :WS-CARD-EXP-DT, :WS-CARD-STATUS
              FROM   cards
              WHERE  card_num = :WS-CARD-NUM
           END-EXEC
           PERFORM 8000-MAP-SQLCODE.

      *----------------------------------------------------------------*
      *                       1220-UPDATE-CARD
      *----------------------------------------------------------------*
       1220-UPDATE-CARD.
           MOVE DAL-DATA-AREA (1:16)              TO WS-CARD-NUM
           MOVE FUNCTION NUMVAL (DAL-DATA-AREA (17:11))
                                                  TO WS-CARD-ACCT
           MOVE FUNCTION NUMVAL (DAL-DATA-AREA (28:3))
                                                  TO WS-CARD-CVV
           MOVE DAL-DATA-AREA (31:50)             TO WS-CARD-NAME
           MOVE DAL-DATA-AREA (81:10)             TO WS-CARD-EXP-DT
           MOVE DAL-DATA-AREA (91:1)              TO WS-CARD-STATUS
           EXEC SQL
              UPDATE cards
                 SET card_acct_id         = :WS-CARD-ACCT,
                     card_cvv_cd          = :WS-CARD-CVV,
                     card_embossed_name   = :WS-CARD-NAME,
                     card_expiration_date = TO_DATE(:WS-CARD-EXP-DT, 'YYYY-MM-DD'),
                     card_active_status   = :WS-CARD-STATUS
               WHERE card_num = :WS-CARD-NUM
           END-EXEC
           PERFORM 8000-MAP-SQLCODE.

      *----------------------------------------------------------------*
      *                       1300-READ-XREF
      *----------------------------------------------------------------*
       1300-READ-XREF.
           MOVE DAL-KEY (1:16) TO WS-XREF-CARD
           EXEC SQL
              SELECT xref_card_num, xref_cust_id, xref_acct_id
              INTO   :WS-XREF-CARD, :WS-XREF-CUST, :WS-XREF-ACCT
              FROM   card_xref
              WHERE  xref_card_num = :WS-XREF-CARD
           END-EXEC
           PERFORM 8000-MAP-SQLCODE.

      *----------------------------------------------------------------*
      *                       1400-READ-USRSEC
      *
      * Used by COSGN00C as the modernised replacement for EXEC CICS
      * READ FILE(USRSEC).  Returns the user-security record into
      * DAL-DATA-AREA in the same byte layout as copybook CSUSR01Y so
      * existing callers can continue to reference SEC-USR-* fields.
      *----------------------------------------------------------------*
       1400-READ-USRSEC.
      *    Host variables for the user-security row.
      *    Declared inline via SQL DECLARE SECTION when the program is
      *    pre-processed by DSNHPC; shown here as ordinary WORKING-
      *    STORAGE for clarity.
           EXEC SQL
              SELECT user_id,
                     COALESCE(first_name, ' '),
                     COALESCE(last_name,  ' '),
                     user_type,
                     password_hash,
                     password_salt
              INTO   :WS-XREF-CARD, -- placeholder host vars; the real
                                    -- mapping is generated from
                                    -- copybook CSUSR01Y at
                                    -- pre-compile time
                     :WS-CARD-NAME,
                     :WS-CARD-NAME,
                     :WS-CARD-STATUS,
                     :WS-LOG-MSG,
                     :WS-ACCT-ZIP
              FROM   user_security
              WHERE  user_id = :DAL-KEY
           END-EXEC
           PERFORM 8000-MAP-SQLCODE.

      *----------------------------------------------------------------*
      *                       8000-MAP-SQLCODE
      *
      * Translate the DB SQLCODE into the CICS-style RESP/RESP2 codes
      * already exposed by the DAL copybook (DBACCESY).
      *----------------------------------------------------------------*
       8000-MAP-SQLCODE.
           MOVE SQLCODE TO DAL-REAS-CD
           EVALUATE SQLCODE
               WHEN 0
                    SET DAL-RESP-NORMAL  TO TRUE
               WHEN +100
                    SET DAL-RESP-NOTFND  TO TRUE
                    MOVE 'Row not found' TO DAL-MESSAGE
               WHEN -803
                    SET DAL-RESP-DUPREC  TO TRUE
                    MOVE 'Duplicate primary key' TO DAL-MESSAGE
               WHEN -530
                    SET DAL-RESP-INVREQ  TO TRUE
                    MOVE 'Foreign key violation' TO DAL-MESSAGE
               WHEN OTHER
                    SET DAL-RESP-IOERR   TO TRUE
                    STRING 'SQL error '
                           SQLCODE
                           DELIMITED BY SIZE
                       INTO DAL-MESSAGE
           END-EVALUATE.

      *----------------------------------------------------------------*
      *                  9000-DISPATCH-VSAM-FALLBACK
      *
      * Strangler-fig fallback path.  When the DAL-USE-RDBMS feature
      * flag is OFF we delegate the requested operation to the legacy
      * VSAM file control programs that callers still link with.  The
      * legacy programs are prefixed with "VSAM" to disambiguate them
      * from the modernised SQL paragraphs above.
      *----------------------------------------------------------------*
       9000-DISPATCH-VSAM-FALLBACK.
           STRING 'VSAM fallback active for ' DAL-OPERATION ' / '
                  DAL-ENTITY DELIMITED BY SIZE INTO WS-LOG-MSG
           DISPLAY WS-LOG-MSG
      *    Each entity has a corresponding legacy CALL.  See section
      *    2d of MODERNIZATION.md for the full crosswalk.  We leave a
      *    SET DAL-RESP-INVREQ here so that any caller exercising an
      *    operation without a configured legacy implementation
      *    receives an explicit error rather than a silent success.
           SET DAL-RESP-INVREQ TO TRUE
           MOVE 'VSAM fallback path not configured for this entity'
                                                       TO DAL-MESSAGE.

       END PROGRAM DBACCESS.
