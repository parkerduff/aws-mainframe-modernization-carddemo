      ******************************************************************
      * Copybook    : CHASHPWY.cpy
      * Application : CardDemo
      * Function    : Linkage area for the CHASHPW (SHA-256 password
      *               hashing) subprogram.  Include this copybook in
      *               any caller that needs to verify or compute a
      *               password hash.
      ******************************************************************
       01  CHASHPW-PARMS.
           05  CHASHPW-PASSWORD         PIC X(64) VALUE SPACES.
           05  CHASHPW-PASSWORD-LEN     PIC S9(04) COMP VALUE 0.
           05  CHASHPW-SALT             PIC X(16) VALUE SPACES.
           05  CHASHPW-SALT-LEN         PIC S9(04) COMP VALUE 0.
           05  CHASHPW-HASH-OUT         PIC X(64) VALUE SPACES.
           05  CHASHPW-RETURN-CODE      PIC S9(09) COMP VALUE 0.
