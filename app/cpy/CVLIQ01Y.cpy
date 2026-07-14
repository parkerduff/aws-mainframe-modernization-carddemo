      *****************************************************************
      *    Intraday Liquidity Inquiry - MQ request/response layouts
      *    Used by COLIQ01 (CICS transaction CDRL)
      *****************************************************************
      *----------------------------------------------------------------*
      *    Request message : account id + intraday time window
      *      LIQ-REQ-FUNC     - Function code, always 'INQL'
      *      LIQ-REQ-ACCT-ID  - 11 digit account identifier
      *      LIQ-REQ-START-TS - Window start ts (TRAN-PROC-TS fmt)
      *      LIQ-REQ-END-TS   - Window end   ts (TRAN-PROC-TS fmt)
      *----------------------------------------------------------------*
       01  LIQ-REQUEST-MSG.
           05  LIQ-REQ-FUNC                 PIC X(04) VALUE 'INQL'.
           05  LIQ-REQ-ACCT-ID              PIC 9(11).
           05  LIQ-REQ-START-TS             PIC X(26).
           05  LIQ-REQ-END-TS               PIC X(26).
           05  FILLER                       PIC X(933).
      *----------------------------------------------------------------*
      *    Response message : intraday liquidity position
      *      LIQ-RESP-OPEN-AVAIL-LIQ - Avail liquidity at window start
      *      LIQ-RESP-INTRADAY-DEBIT - Sum of debit  amounts in window
      *      LIQ-RESP-INTRADAY-CREDIT- Sum of credit amounts in window
      *      LIQ-RESP-INTRADAY-NET   - Net liq change (credit-debit)
      *      LIQ-RESP-CURR-AVAIL-LIQ - Available liquidity as of now
      *      LIQ-RESP-AVAIL-CASH     - Available cash credit
      *      LIQ-RESP-RETURN-CODE    - 00 ok, 01 notfnd, 02 bad req
      *----------------------------------------------------------------*
       01  LIQ-RESPONSE-MSG.
           05  LIQ-RESP-ACCT-ID             PIC 9(11).
           05  LIQ-RESP-AS-OF-TS            PIC X(26).
           05  LIQ-RESP-OPEN-AVAIL-LIQ      PIC S9(10)V99.
           05  LIQ-RESP-INTRADAY-DEBIT      PIC S9(12)V99.
           05  LIQ-RESP-INTRADAY-CREDIT     PIC S9(12)V99.
           05  LIQ-RESP-INTRADAY-NET        PIC S9(12)V99.
           05  LIQ-RESP-CURR-AVAIL-LIQ      PIC S9(10)V99.
           05  LIQ-RESP-AVAIL-CASH          PIC S9(10)V99.
           05  LIQ-RESP-RETURN-CODE         PIC X(02).
           05  LIQ-RESP-STATUS-MSG          PIC X(50).
      *
      * Ver: CardDemo - Intraday Liquidity MQ extension
      *
