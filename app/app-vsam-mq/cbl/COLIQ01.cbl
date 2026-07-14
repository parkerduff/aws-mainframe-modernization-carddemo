       IDENTIFICATION DIVISION.
       PROGRAM-ID.           COLIQ01 IS INITIAL.
       AUTHOR.               AWS.
      ******************************************************************
      * Program     : COLIQ01.CBL
      * Application : CardDemo - MQ/VSAM extension
      * Type        : CICS COBOL Program (MQ triggered)
      * Function    : Intraday liquidity inquiry via MQ.
      *               Reads a request message (account id + intraday
      *               time window) from the request queue,
      *               computes the account's intraday liquidity position
      *               from the ACCTDATA and TRANSACT VSAM files and puts
      *               a response message on CARD.DEMO.REPLY.LIQ.
      *               Modeled on COACCT01.CBL.
      ******************************************************************

       ENVIRONMENT DIVISION.

       DATA DIVISION.

       WORKING-STORAGE SECTION.

       01 WS-MQ-MSG-FLAG                PIC X(01) VALUE 'N'.
          88  NO-MORE-MSGS              VALUE 'Y'.

       01 WS-RESP-QUEUE-STS            PIC X(01) VALUE 'N'.
          88  RESP-QUEUE-OPEN          VALUE 'Y'.

       01 WS-ERR-QUEUE-STS             PIC X(01) VALUE 'N'.
          88  ERR-QUEUE-OPEN          VALUE 'Y'.

       01 WS-REPLY-QUEUE-STS           PIC X(01) VALUE 'N'.
          88  REPLY-QUEUE-OPEN         VALUE 'Y'.

       01 WS-TRANSACT-EOF              PIC X(01) VALUE 'N'.
          88  TRANSACT-EOF             VALUE 'Y'.
          88  TRANSACT-NOT-EOF         VALUE 'N'.

       01 WS-CICS-RESP-CDS.
          05  WS-CICS-RESP1-CD        PIC S9(08) COMP VALUE ZERO.
          05  WS-CICS-RESP2-CD        PIC S9(08) COMP VALUE ZERO.
          05  WS-CICS-RESP1-CD-D      PIC 9(08) VALUE ZERO.
          05  WS-CICS-RESP2-CD-D      PIC 9(08) VALUE ZERO.

      ***********************************************
      **             DATE FIELDS                   **
      ***********************************************
       01 WS-DATE-TIME.
          10 WS-ABS-TIME                  PIC S9(15) COMP-3 VALUE ZERO.
          10 WS-MMDDYYYY                  PIC X(10) VALUE SPACES.
          10 WS-TIME                      PIC X(8)  VALUE SPACES.
      ***********************************************
      **             MQ FIELDS                     **
      ***********************************************
       01 MQ-QUEUE                        PIC X(48).
       01 MQ-QUEUE-REPLY                  PIC X(48).
       01 MQ-HCONN                        PIC S9(09) BINARY VALUE 0.
       01 MQ-CONDITION-CODE               PIC S9(09) BINARY VALUE 0.
       01 MQ-REASON-CODE                  PIC S9(09) BINARY VALUE 0.
       01 MQ-HOBJ                         PIC S9(09) BINARY VALUE 0.
       01 MQ-OPTIONS                      PIC S9(09) BINARY VALUE 0.
       01 MQ-BUFFER-LENGTH                PIC S9(09) BINARY.
       01 MQ-BUFFER                       PIC X(1000).
       01 MQ-DATA-LENGTH                  PIC S9(09) BINARY.
       01 MQ-CORRELID                     PIC X(24).
       01 MQ-MSG-ID                       PIC X(24).
       01 MQ-MSG-COUNT                    PIC 9(09).
       01 SAVE-CORELID                    PIC X(24).
       01 SAVE-MSGID                      PIC X(24).
       01 SAVE-REPLY2Q                    PIC X(48).
       01 MQ-ERR-DISPLAY.
           05 MQ-ERROR-PARA                   PIC X(25) .
           05 FILLER                          PIC X(02) VALUE SPACES.
           05 MQ-APPL-RETURN-MESSAGE          PIC X(25).
           05 FILLER                          PIC X(02) VALUE SPACES.
           05 MQ-APPL-CONDITION-CODE          PIC 9(02).
           05 FILLER                          PIC X(02) VALUE SPACES.
           05 MQ-APPL-REASON-CODE             PIC 9(05).
           05 FILLER                          PIC X(02) VALUE SPACES.
           05 MQ-APPL-QUEUE-NAME              PIC X(48).


       01 MQ-GET-MESSAGE-OPTIONS.
       COPY CMQGMOV.


       01 MQ-PUT-MESSAGE-OPTIONS.
       COPY CMQPMOV.


       01 MQ-MESSAGE-DESCRIPTOR.
       COPY CMQMDV.


       01 MQ-OBJECT-DESCRIPTOR.
       COPY CMQODV.


       01 MQ-CONSTANTS.
       COPY CMQV.

       01 MQ-GET-QUEUE-MESSAGE.
       COPY CMQTML.

       01  QUEUE-INFO.
           05 QMGR-NAME                   PIC X(48) VALUE SPACES.
           05 INPUT-QUEUE-NAME            PIC X(48) VALUE SPACES.
           05 REPLY-QUEUE-NAME            PIC X(48) VALUE SPACES.
           05 ERROR-QUEUE-NAME            PIC X(48) VALUE SPACES.

       01 INPUT-QUEUE-HANDLE              PIC S9(09) BINARY VALUE 0.

       01 OUTPUT-QUEUE-HANDLE             PIC S9(09) BINARY VALUE 0.

       01 ERROR-QUEUE-HANDLE              PIC S9(09) BINARY VALUE 0.

       01 QMGR-HANDLE-CONN                PIC S9(09) BINARY VALUE 0.
       01 QUEUE-MESSAGE                   PIC X(1000).
       01 REQUEST-MESSAGE                 PIC X(1000).
       01 REPLY-MESSAGE                   PIC X(1000).
       01 ERROR-MESSAGE                   PIC X(1000).

       01 WS-VARIABLES.
          05 LIT-ACCTFILENAME                      PIC X(8)
                                                   VALUE 'ACCTDAT '.
          05 LIT-TRANFILENAME                      PIC X(8)
                                                   VALUE 'TRANSACT'.
          05 WS-RESP-CD                          PIC S9(09) COMP
                                                   VALUE ZEROS.
          05 WS-REAS-CD                          PIC S9(09) COMP
                                                   VALUE ZEROS.
          05  WS-XREF-RID.
            10  WS-CARD-RID-ACCT-ID                 PIC 9(11).
            10  WS-CARD-RID-ACCT-ID-X REDEFINES
                   WS-CARD-RID-ACCT-ID              PIC X(11).

      *  Intraday liquidity accumulators
       01 WS-CALC-FIELDS.
          05 WS-INTRADAY-DEBIT                  PIC S9(12)V99
                                                   VALUE ZEROS.
          05 WS-INTRADAY-CREDIT                 PIC S9(12)V99
                                                   VALUE ZEROS.
          05 WS-NET-BAL-CHANGE                  PIC S9(12)V99
                                                   VALUE ZEROS.
          05 WS-OPEN-AVAIL-LIQ                  PIC S9(10)V99
                                                   VALUE ZEROS.
          05 WS-CURR-AVAIL-LIQ                  PIC S9(10)V99
                                                   VALUE ZEROS.
          05 WS-AVAIL-CASH                      PIC S9(10)V99
                                                   VALUE ZEROS.

      *  Request message copy (see CVLIQ01Y for field descriptions)
       COPY CVLIQ01Y.

      *  Account record layout
       COPY CVACT01Y.

      *  Transaction record layout
       COPY CVTRA05Y.

       LINKAGE SECTION.

       PROCEDURE DIVISION.

       1000-CONTROL.

            MOVE SPACES TO
                          INPUT-QUEUE-NAME
                          QMGR-NAME
                          QUEUE-MESSAGE

            INITIALIZE MQ-ERR-DISPLAY

           PERFORM 2100-OPEN-ERROR-QUEUE
      ******************************************************************
      * GET THE QUEUE NAME WHICH STARTED THE TRANSACTION               *
      ******************************************************************
           EXEC CICS RETRIEVE
             INTO(MQTM)
             RESP(WS-CICS-RESP1-CD)
             RESP2(WS-CICS-RESP2-CD)
           END-EXEC
           IF WS-CICS-RESP1-CD =  DFHRESP(NORMAL)
             MOVE MQTM-QNAME  TO INPUT-QUEUE-NAME
             MOVE 'CARD.DEMO.REPLY.LIQ' TO REPLY-QUEUE-NAME
           ELSE
             MOVE 'CICS RETREIVE' TO MQ-ERROR-PARA
             MOVE WS-CICS-RESP1-CD TO WS-CICS-RESP1-CD-D
             MOVE WS-CICS-RESP2-CD TO WS-CICS-RESP2-CD
             STRING 'RESP: ', WS-CICS-RESP1-CD-D , WS-CICS-RESP2-CD-D,
                    'END' DELIMITED BY SIZE
                    INTO MQ-APPL-RETURN-MESSAGE
             END-STRING

             PERFORM 9000-ERROR
             PERFORM 8000-TERMINATION
           END-IF

           PERFORM 2300-OPEN-INPUT-QUEUE
           PERFORM 2400-OPEN-OUTPUT-QUEUE
           PERFORM 3000-GET-REQUEST
           PERFORM 4000-MAIN-PROCESS UNTIL
                   NO-MORE-MSGS

           PERFORM 8000-TERMINATION.

           .

       2300-OPEN-INPUT-QUEUE.
      * OPEN-INPUT WILL OPEN A QUEUE FOR GET PROCESSING

           MOVE SPACES           TO MQOD-OBJECTQMGRNAME
           MOVE INPUT-QUEUE-NAME TO MQOD-OBJECTNAME

           COMPUTE MQ-OPTIONS = MQOO-INPUT-SHARED
                              + MQOO-SAVE-ALL-CONTEXT
                              + MQOO-FAIL-IF-QUIESCING

           CALL 'MQOPEN' USING QMGR-HANDLE-CONN
                               MQ-OBJECT-DESCRIPTOR
                               MQ-OPTIONS
                               MQ-HOBJ
                               MQ-CONDITION-CODE
                               MQ-REASON-CODE

           EVALUATE MQ-CONDITION-CODE
               WHEN MQCC-OK
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE MQ-HOBJ           TO INPUT-QUEUE-HANDLE
                    SET  REPLY-QUEUE-OPEN  TO TRUE
               WHEN OTHER
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE INPUT-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                    MOVE 'INP MQOPEN ERR'  TO MQ-APPL-RETURN-MESSAGE
                    PERFORM 9000-ERROR
                    PERFORM 8000-TERMINATION
           END-EVALUATE.

       2400-OPEN-OUTPUT-QUEUE.

      * OPEN-OUTPUT WILL OPEN A QUEUE FOR PUT PROCESSING

           MOVE SPACES            TO MQOD-OBJECTQMGRNAME
           MOVE REPLY-QUEUE-NAME  TO MQOD-OBJECTNAME

           COMPUTE MQ-OPTIONS = MQOO-OUTPUT
                              + MQOO-PASS-ALL-CONTEXT
                              + MQOO-FAIL-IF-QUIESCING

           CALL 'MQOPEN' USING QMGR-HANDLE-CONN
                               MQ-OBJECT-DESCRIPTOR
                               MQ-OPTIONS
                               MQ-HOBJ
                               MQ-CONDITION-CODE
                               MQ-REASON-CODE

           EVALUATE MQ-CONDITION-CODE
               WHEN MQCC-OK
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE MQ-HOBJ           TO OUTPUT-QUEUE-HANDLE
                    SET  RESP-QUEUE-OPEN   TO TRUE
               WHEN OTHER
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE REPLY-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                    MOVE 'OUT MQOPEN ERR'  TO MQ-APPL-RETURN-MESSAGE
                    PERFORM 9000-ERROR
                    PERFORM 8000-TERMINATION
           END-EVALUATE.

       2100-OPEN-ERROR-QUEUE.

      * OPEN-OUTPUT WILL OPEN A QUEUE FOR PUT PROCESSING

           MOVE 'CARD.DEMO.ERROR' TO ERROR-QUEUE-NAME
           MOVE SPACES            TO MQOD-OBJECTQMGRNAME
           MOVE ERROR-QUEUE-NAME  TO MQOD-OBJECTNAME

           COMPUTE MQ-OPTIONS = MQOO-OUTPUT
                              + MQOO-PASS-ALL-CONTEXT
                              + MQOO-FAIL-IF-QUIESCING

           CALL 'MQOPEN' USING QMGR-HANDLE-CONN
                               MQ-OBJECT-DESCRIPTOR
                               MQ-OPTIONS
                               MQ-HOBJ
                               MQ-CONDITION-CODE
                               MQ-REASON-CODE

           EVALUATE MQ-CONDITION-CODE
               WHEN MQCC-OK
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE MQ-HOBJ           TO ERROR-QUEUE-HANDLE
                    SET  ERR-QUEUE-OPEN   TO TRUE
               WHEN OTHER
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE ERROR-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                    MOVE 'ERR MQOPEN ERR'  TO MQ-APPL-RETURN-MESSAGE
                    DISPLAY MQ-ERR-DISPLAY
                    PERFORM 8000-TERMINATION
           END-EVALUATE.


       4000-MAIN-PROCESS.
           EXEC CICS
                SYNCPOINT
           END-EXEC

           PERFORM 3000-GET-REQUEST
           .


       3000-GET-REQUEST.
      * GET WILL GET A MESSAGE FROM THE QUEUE
      *** ADDED 5000 MS (5 SECS) AS THE WAIT INTERVAL FOR GET
           MOVE 5000                            TO MQGMO-WAITINTERVAL
           MOVE SPACES                          TO MQ-CORRELID
           MOVE SPACES                          TO MQ-MSG-ID
           MOVE INPUT-QUEUE-NAME                TO MQ-QUEUE
           MOVE INPUT-QUEUE-HANDLE              TO MQ-HOBJ
           MOVE 1000                            TO MQ-BUFFER-LENGTH
           MOVE MQMI-NONE         TO MQMD-MSGID
           MOVE MQCI-NONE         TO MQMD-CORRELID
           INITIALIZE LIQ-REQUEST-MSG  REPLACING NUMERIC BY ZEROES

           COMPUTE MQGMO-OPTIONS = MQGMO-SYNCPOINT
                                 + MQGMO-FAIL-IF-QUIESCING
                                 + MQGMO-CONVERT
                                 + MQGMO-WAIT

           CALL 'MQGET'  USING MQ-HCONN
                               MQ-HOBJ
                               MQ-MESSAGE-DESCRIPTOR
                               MQ-GET-MESSAGE-OPTIONS
                               MQ-BUFFER-LENGTH
                               MQ-BUFFER
                               MQ-DATA-LENGTH
                               MQ-CONDITION-CODE
                               MQ-REASON-CODE


           IF MQ-CONDITION-CODE = MQCC-OK
              MOVE MQMD-MSGID        TO MQ-MSG-ID
              MOVE MQMD-CORRELID     TO MQ-CORRELID
              MOVE MQMD-REPLYTOQ     TO MQ-QUEUE-REPLY
              MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
              MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
              MOVE MQ-BUFFER         TO REQUEST-MESSAGE
              MOVE MQ-CORRELID       TO SAVE-CORELID
              MOVE MQ-QUEUE-REPLY    TO SAVE-REPLY2Q
              MOVE MQ-MSG-ID         TO SAVE-MSGID
              MOVE REQUEST-MESSAGE   TO LIQ-REQUEST-MSG
              PERFORM 4000-PROCESS-REQUEST-REPLY
              ADD  1                 TO MQ-MSG-COUNT
           ELSE
              IF MQ-REASON-CODE  =  MQRC-NO-MSG-AVAILABLE
                SET NO-MORE-MSGS             TO  TRUE

              ELSE
                 MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                 MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                 MOVE INPUT-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                 MOVE 'INP MQGET ERR:'  TO MQ-APPL-RETURN-MESSAGE
                 PERFORM 9000-ERROR
                 PERFORM 8000-TERMINATION
             END-IF
           END-IF.

       4000-PROCESS-REQUEST-REPLY.
           MOVE SPACES TO REPLY-MESSAGE
           INITIALIZE LIQ-RESPONSE-MSG REPLACING NUMERIC BY ZEROES
           MOVE ZEROS TO WS-INTRADAY-DEBIT
                         WS-INTRADAY-CREDIT
                         WS-NET-BAL-CHANGE
           IF LIQ-REQ-FUNC = 'INQL' AND LIQ-REQ-ACCT-ID > ZEROES
              MOVE LIQ-REQ-ACCT-ID  TO  WS-CARD-RID-ACCT-ID

           EXEC CICS READ
                DATASET   (LIT-ACCTFILENAME)
                RIDFLD    (WS-CARD-RID-ACCT-ID-X)
                KEYLENGTH (LENGTH OF WS-CARD-RID-ACCT-ID-X)
                INTO      (ACCOUNT-RECORD)
                LENGTH    (LENGTH OF ACCOUNT-RECORD)
                RESP      (WS-RESP-CD)
                RESP2     (WS-REAS-CD)
           END-EXEC

           EVALUATE WS-RESP-CD
               WHEN DFHRESP(NORMAL)
                    PERFORM 4200-SUM-INTRADAY-TRANS
                    COMPUTE WS-CURR-AVAIL-LIQ =
                            ACCT-CREDIT-LIMIT - ACCT-CURR-BAL
                    COMPUTE WS-OPEN-AVAIL-LIQ =
                            WS-CURR-AVAIL-LIQ + WS-NET-BAL-CHANGE
                    COMPUTE WS-AVAIL-CASH =
                            ACCT-CASH-CREDIT-LIMIT
                            - ACCT-CURR-CYC-DEBIT
                    MOVE ACCT-ID          TO LIQ-RESP-ACCT-ID
                    MOVE LIQ-REQ-END-TS   TO LIQ-RESP-AS-OF-TS
                    MOVE WS-OPEN-AVAIL-LIQ
                                          TO LIQ-RESP-OPEN-AVAIL-LIQ
                    MOVE WS-INTRADAY-DEBIT
                                          TO LIQ-RESP-INTRADAY-DEBIT
                    MOVE WS-INTRADAY-CREDIT
                                          TO LIQ-RESP-INTRADAY-CREDIT
                    COMPUTE LIQ-RESP-INTRADAY-NET =
                            WS-INTRADAY-CREDIT - WS-INTRADAY-DEBIT
                    MOVE WS-CURR-AVAIL-LIQ
                                          TO LIQ-RESP-CURR-AVAIL-LIQ
                    MOVE WS-AVAIL-CASH    TO LIQ-RESP-AVAIL-CASH
                    MOVE '00'             TO LIQ-RESP-RETURN-CODE
                    MOVE 'LIQUIDITY INQUIRY SUCCESSFUL'
                                          TO LIQ-RESP-STATUS-MSG
                    MOVE LIQ-RESPONSE-MSG TO REPLY-MESSAGE
                    PERFORM 4100-PUT-REPLY
               WHEN DFHRESP(NOTFND)
                    MOVE LIQ-REQ-ACCT-ID  TO LIQ-RESP-ACCT-ID
                    MOVE '01'             TO LIQ-RESP-RETURN-CODE
                    MOVE 'ACCOUNT NOT FOUND'
                                          TO LIQ-RESP-STATUS-MSG
                    MOVE LIQ-RESPONSE-MSG TO REPLY-MESSAGE
                    PERFORM 4100-PUT-REPLY
      *
               WHEN OTHER

                  MOVE WS-RESP-CD        TO MQ-APPL-CONDITION-CODE
                  MOVE WS-REAS-CD        TO MQ-APPL-REASON-CODE
                  MOVE INPUT-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                  MOVE 'ERROR WHILE READING ACCTFILE'
                                         TO MQ-APPL-RETURN-MESSAGE
                  PERFORM 9000-ERROR
                  PERFORM 8000-TERMINATION
           END-EVALUATE
           ELSE
                    MOVE LIQ-REQ-ACCT-ID  TO LIQ-RESP-ACCT-ID
                    MOVE '02'             TO LIQ-RESP-RETURN-CODE
                    MOVE 'INVALID REQUEST PARAMETERS'
                                          TO LIQ-RESP-STATUS-MSG
                    MOVE LIQ-RESPONSE-MSG TO REPLY-MESSAGE
                    PERFORM 4100-PUT-REPLY
           END-IF
           .

      *----------------------------------------------------------------*
      *  Browse TRANSACT and accumulate amounts whose TRAN-PROC-TS
      *  falls within the requested intraday window (full timestamp
      *  granularity range compare, per CBTRN03C technique).
      *----------------------------------------------------------------*
       4200-SUM-INTRADAY-TRANS.
           SET TRANSACT-NOT-EOF TO TRUE
           MOVE LOW-VALUES TO TRAN-ID
           PERFORM 4210-STARTBR-TRANSACT
           IF WS-RESP-CD = DFHRESP(NORMAL)
              PERFORM UNTIL TRANSACT-EOF
                 PERFORM 4220-READNEXT-TRANSACT
                 IF TRANSACT-NOT-EOF
                    IF TRAN-PROC-TS (1:26) >= LIQ-REQ-START-TS
                       AND TRAN-PROC-TS (1:26) <= LIQ-REQ-END-TS
                       ADD TRAN-AMT TO WS-NET-BAL-CHANGE
                       IF TRAN-AMT >= ZERO
                          ADD TRAN-AMT TO WS-INTRADAY-DEBIT
                       ELSE
                          COMPUTE WS-INTRADAY-CREDIT =
                                  WS-INTRADAY-CREDIT - TRAN-AMT
                       END-IF
                    END-IF
                 END-IF
              END-PERFORM
              PERFORM 4230-ENDBR-TRANSACT
           END-IF
           .

       4210-STARTBR-TRANSACT.
           EXEC CICS STARTBR
                DATASET   (LIT-TRANFILENAME)
                RIDFLD    (TRAN-ID)
                KEYLENGTH (LENGTH OF TRAN-ID)
                GTEQ
                RESP      (WS-RESP-CD)
                RESP2     (WS-REAS-CD)
           END-EXEC
           EVALUATE WS-RESP-CD
               WHEN DFHRESP(NORMAL)
                    CONTINUE
               WHEN DFHRESP(NOTFND)
                    SET TRANSACT-EOF TO TRUE
               WHEN OTHER
                    SET TRANSACT-EOF TO TRUE
           END-EVALUATE
           .

       4220-READNEXT-TRANSACT.
           EXEC CICS READNEXT
                DATASET   (LIT-TRANFILENAME)
                INTO      (TRAN-RECORD)
                LENGTH    (LENGTH OF TRAN-RECORD)
                RIDFLD    (TRAN-ID)
                KEYLENGTH (LENGTH OF TRAN-ID)
                RESP      (WS-RESP-CD)
                RESP2     (WS-REAS-CD)
           END-EXEC
           EVALUATE WS-RESP-CD
               WHEN DFHRESP(NORMAL)
                    CONTINUE
               WHEN DFHRESP(ENDFILE)
                    SET TRANSACT-EOF TO TRUE
               WHEN OTHER
                    SET TRANSACT-EOF TO TRUE
           END-EVALUATE
           .

       4230-ENDBR-TRANSACT.
           EXEC CICS ENDBR
                DATASET   (LIT-TRANFILENAME)
                RESP      (WS-RESP-CD)
                RESP2     (WS-REAS-CD)
           END-EXEC
           .

       4100-PUT-REPLY.

      * PUT WILL PUT A MESSAGE ON THE QUEUE AND CONVERT IT TO A STRING

           MOVE REPLY-MESSAGE                TO MQ-BUFFER
           MOVE 1000                         TO MQ-BUFFER-LENGTH
           MOVE SAVE-MSGID                   TO MQMD-MSGID
           MOVE SAVE-CORELID                 TO MQMD-CORRELID
           MOVE MQFMT-STRING                 TO MQMD-FORMAT

           COMPUTE MQMD-CODEDCHARSETID      =  MQCCSI-Q-MGR

           COMPUTE MQPMO-OPTIONS = MQPMO-SYNCPOINT
                                 + MQPMO-DEFAULT-CONTEXT
                                 + MQPMO-FAIL-IF-QUIESCING

           CALL 'MQPUT'  USING MQ-HCONN
                               OUTPUT-QUEUE-HANDLE
                               MQ-MESSAGE-DESCRIPTOR
                               MQ-PUT-MESSAGE-OPTIONS
                               MQ-BUFFER-LENGTH
                               MQ-BUFFER
                               MQ-CONDITION-CODE
                               MQ-REASON-CODE

           EVALUATE MQ-CONDITION-CODE
               WHEN MQCC-OK
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
               WHEN OTHER
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE REPLY-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                    MOVE 'MQPUT ERR'       TO MQ-APPL-RETURN-MESSAGE
                    PERFORM 9000-ERROR
                    PERFORM 8000-TERMINATION
           END-EVALUATE.

       9000-ERROR.
      * PUT WILL PUT A MESSAGE ON THE QUEUE AND CONVERT IT TO A STRING

           MOVE MQ-ERR-DISPLAY               TO ERROR-MESSAGE,
           MOVE ERROR-MESSAGE                TO MQ-BUFFER
           MOVE 1000                         TO MQ-BUFFER-LENGTH
           MOVE MQFMT-STRING                 TO MQMD-FORMAT

           COMPUTE MQMD-CODEDCHARSETID      =  MQCCSI-Q-MGR

           COMPUTE MQPMO-OPTIONS = MQPMO-SYNCPOINT
                                 + MQPMO-DEFAULT-CONTEXT
                                 + MQPMO-FAIL-IF-QUIESCING

           CALL 'MQPUT'  USING MQ-HCONN
                               ERROR-QUEUE-HANDLE
                               MQ-MESSAGE-DESCRIPTOR
                               MQ-PUT-MESSAGE-OPTIONS
                               MQ-BUFFER-LENGTH
                               MQ-BUFFER
                               MQ-CONDITION-CODE
                               MQ-REASON-CODE

           EVALUATE MQ-CONDITION-CODE
               WHEN MQCC-OK
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
               WHEN OTHER
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE ERROR-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                    MOVE 'MQPUT ERR'       TO MQ-APPL-RETURN-MESSAGE
                    DISPLAY MQ-ERR-DISPLAY
                    PERFORM 8000-TERMINATION
           END-EVALUATE.
           .

       8000-TERMINATION.

           IF REPLY-QUEUE-OPEN
              PERFORM 5000-CLOSE-INPUT-QUEUE
           END-IF
           IF RESP-QUEUE-OPEN
              PERFORM 5100-CLOSE-OUTPUT-QUEUE
           END-IF
           IF ERR-QUEUE-OPEN
              PERFORM 5200-CLOSE-ERROR-QUEUE
           END-IF
           EXEC CICS RETURN END-EXEC
           GOBACK.

       5000-CLOSE-INPUT-QUEUE.
           MOVE INPUT-QUEUE-NAME           TO MQ-QUEUE
           MOVE INPUT-QUEUE-HANDLE         TO MQ-HOBJ
           COMPUTE MQ-OPTIONS = MQCO-NONE

           CALL 'MQCLOSE' USING MQ-HCONN
                                MQ-HOBJ
                                MQ-OPTIONS
                                MQ-CONDITION-CODE
                                MQ-REASON-CODE

           EVALUATE MQ-CONDITION-CODE
               WHEN MQCC-OK
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
               WHEN OTHER
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE INPUT-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                    MOVE 'MQCLOSE ERR'     TO MQ-APPL-RETURN-MESSAGE
                    PERFORM 8000-TERMINATION
           END-EVALUATE.
       5100-CLOSE-OUTPUT-QUEUE.
           MOVE REPLY-QUEUE-NAME            TO MQ-QUEUE
           MOVE OUTPUT-QUEUE-HANDLE         TO MQ-HOBJ
           COMPUTE MQ-OPTIONS = MQCO-NONE

           CALL 'MQCLOSE' USING MQ-HCONN
                                MQ-HOBJ
                                MQ-OPTIONS
                                MQ-CONDITION-CODE
                                MQ-REASON-CODE

           EVALUATE MQ-CONDITION-CODE
               WHEN MQCC-OK
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
               WHEN OTHER
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE INPUT-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                    MOVE 'MQCLOSE ERR'     TO MQ-APPL-RETURN-MESSAGE
                    PERFORM 8000-TERMINATION
           END-EVALUATE.

       5200-CLOSE-ERROR-QUEUE.
           MOVE ERROR-QUEUE-NAME          TO MQ-QUEUE
           MOVE ERROR-QUEUE-HANDLE         TO MQ-HOBJ
           COMPUTE MQ-OPTIONS = MQCO-NONE

           CALL 'MQCLOSE' USING MQ-HCONN
                                MQ-HOBJ
                                MQ-OPTIONS
                                MQ-CONDITION-CODE
                                MQ-REASON-CODE

           EVALUATE MQ-CONDITION-CODE
               WHEN MQCC-OK
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
               WHEN OTHER
                    MOVE MQ-CONDITION-CODE TO MQ-APPL-CONDITION-CODE
                    MOVE MQ-REASON-CODE    TO MQ-APPL-REASON-CODE
                    MOVE ERROR-QUEUE-NAME  TO MQ-APPL-QUEUE-NAME
                    MOVE 'MQCLOSE ERR'     TO MQ-APPL-RETURN-MESSAGE
                    PERFORM 9000-ERROR
                    PERFORM 8000-TERMINATION
           END-EVALUATE.
