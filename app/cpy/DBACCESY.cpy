      ******************************************************************
      * Copybook    : DBACCESY.cpy
      * Application : CardDemo
      * Function    : Linkage area for the DBACCESS data-access-layer
      *               subprogram.  Callers use this copybook to ask the
      *               DAL to read, browse, write, update or delete the
      *               core CardDemo entities (account, card, customer,
      *               card cross-reference, transaction).  The DAL
      *               isolates SQL/CICS specifics from the business
      *               programs.
      ******************************************************************
       01  DAL-COMMAREA.
           05  DAL-OPERATION              PIC X(08).
              88 DAL-OP-READ              VALUE 'READ    '.
              88 DAL-OP-READ-NEXT         VALUE 'READNEXT'.
              88 DAL-OP-WRITE             VALUE 'WRITE   '.
              88 DAL-OP-UPDATE            VALUE 'UPDATE  '.
              88 DAL-OP-DELETE            VALUE 'DELETE  '.
              88 DAL-OP-START-BR          VALUE 'STARTBR '.
              88 DAL-OP-END-BR            VALUE 'ENDBR   '.

           05  DAL-ENTITY                 PIC X(08).
              88 DAL-EN-ACCOUNT           VALUE 'ACCOUNT '.
              88 DAL-EN-CARD              VALUE 'CARD    '.
              88 DAL-EN-CUSTOMER          VALUE 'CUSTOMER'.
              88 DAL-EN-CARDXREF          VALUE 'CARDXREF'.
              88 DAL-EN-TRANSACT          VALUE 'TRANSACT'.
              88 DAL-EN-USRSEC            VALUE 'USRSEC  '.
              88 DAL-EN-DALYTRAN          VALUE 'DALYTRAN'.
              88 DAL-EN-TCATBAL           VALUE 'TCATBAL '.

           05  DAL-KEY                    PIC X(32) VALUE SPACES.
           05  DAL-KEY-LEN                PIC S9(04) COMP VALUE 0.

           05  DAL-DATA-LEN               PIC S9(09) COMP VALUE 0.
           05  DAL-DATA-AREA              PIC X(2048) VALUE SPACES.

      *    Status returned to the caller.  Mirrors the EIBRESP /
      *    EIBRESP2 convention so existing CICS-based callers can
      *    continue to use familiar EVALUATE patterns.
           05  DAL-RESP-CD                PIC S9(09) COMP VALUE 0.
              88 DAL-RESP-NORMAL          VALUE 0.
              88 DAL-RESP-NOTFND          VALUE 13.
              88 DAL-RESP-DUPREC          VALUE 14.
              88 DAL-RESP-INVREQ          VALUE 16.
              88 DAL-RESP-NOSPACE         VALUE 18.
              88 DAL-RESP-IOERR           VALUE 17.
              88 DAL-RESP-ENDFILE         VALUE 20.
           05  DAL-REAS-CD                PIC S9(09) COMP VALUE 0.
           05  DAL-MESSAGE                PIC X(80)  VALUE SPACES.
