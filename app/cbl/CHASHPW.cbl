      ******************************************************************
      * Program     : CHASHPW.CBL
      * Application : CardDemo
      * Type        : COBOL Subprogram (callable from batch and CICS)
      * Function    : Compute SHA-256(password || salt) and return the
      *               64-byte lowercase hexadecimal digest.  Used by
      *               COSGN00C and the user-security maintenance
      *               programs to keep plaintext passwords out of the
      *               security file and out of comparison logic.
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
       PROGRAM-ID.    CHASHPW.
       AUTHOR.        AWS-MODERNIZATION.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      *
      * --- ICSF One-Way-Hash interface (CSNBOWH / CSNEOWH) ----------
      * On z/OS the canonical implementation calls the ICSF
      * (Integrated Cryptographic Service Facility) One-Way-Hash
      * generate service to perform SHA-256.  The same source can be
      * compiled on AWS Mainframe Modernization where the runtime
      * exposes an equivalent service through the Java callable
      * interface CSNBOWH.  Distributed builds may also link against
      * a pure-COBOL SHA-256 implementation supplied by the standard
      * cryptographic copybook library (not included in this repo).
      *
       01  WS-ICSF-PARMS.
           05  WS-RETURN-CODE          PIC S9(09) COMP   VALUE 0.
           05  WS-REASON-CODE          PIC S9(09) COMP   VALUE 0.
           05  WS-EXIT-DATA-LEN        PIC S9(09) COMP   VALUE 0.
           05  WS-EXIT-DATA            PIC X(04)         VALUE LOW-VALUES.
           05  WS-RULE-ARRAY-COUNT     PIC S9(09) COMP   VALUE 2.
           05  WS-RULE-ARRAY.
               10 WS-RULE-1            PIC X(08)         VALUE 'SHA-256 '.
               10 WS-RULE-2            PIC X(08)         VALUE 'ONLY    '.
           05  WS-TEXT-LENGTH          PIC S9(09) COMP   VALUE 0.
           05  WS-CHAIN-VAR-LEN        PIC S9(09) COMP   VALUE 32.
           05  WS-CHAIN-VAR            PIC X(32)         VALUE LOW-VALUES.
           05  WS-HASH-LENGTH          PIC S9(09) COMP   VALUE 32.
           05  WS-HASH-VALUE           PIC X(32)         VALUE LOW-VALUES.

       01  WS-WORK-AREA.
           05  WS-INPUT-BUFFER         PIC X(1024)       VALUE SPACES.
           05  WS-INPUT-LEN            PIC S9(09) COMP   VALUE 0.
           05  WS-IDX                  PIC S9(04) COMP   VALUE 0.
           05  WS-IDX-OUT              PIC S9(04) COMP   VALUE 0.
           05  WS-NIBBLE                PIC 9(02)        VALUE 0.
           05  WS-HEX-DIGITS            PIC X(16)
                                       VALUE '0123456789abcdef'.
           05  WS-BYTE-VALUE            PIC 9(03)        VALUE 0.
           05  WS-BYTE-CHAR             PIC X            VALUE SPACE.

      *----------------------------------------------------------------*
      *                        LINKAGE SECTION
      *----------------------------------------------------------------*
       LINKAGE SECTION.
       01  LK-PARMS.
           05  LK-PASSWORD              PIC X(64).
           05  LK-PASSWORD-LEN          PIC S9(04) COMP.
           05  LK-SALT                  PIC X(16).
           05  LK-SALT-LEN              PIC S9(04) COMP.
           05  LK-HASH-OUT              PIC X(64).
           05  LK-RETURN-CODE           PIC S9(09) COMP.

       PROCEDURE DIVISION USING LK-PARMS.
       0000-MAIN.
           MOVE 0          TO LK-RETURN-CODE
           MOVE LOW-VALUES TO WS-INPUT-BUFFER
           MOVE LOW-VALUES TO WS-HASH-VALUE
           MOVE SPACES     TO LK-HASH-OUT

      *    Concatenate password || salt into the input buffer.
           MOVE LK-PASSWORD (1:LK-PASSWORD-LEN)
                                       TO WS-INPUT-BUFFER (1:LK-PASSWORD-LEN)
           IF LK-SALT-LEN > 0
               COMPUTE WS-IDX = LK-PASSWORD-LEN + 1
               MOVE LK-SALT (1:LK-SALT-LEN)
                                       TO WS-INPUT-BUFFER (WS-IDX:LK-SALT-LEN)
           END-IF
           COMPUTE WS-INPUT-LEN = LK-PASSWORD-LEN + LK-SALT-LEN
           MOVE WS-INPUT-LEN           TO WS-TEXT-LENGTH

      *    Invoke ICSF SHA-256 one-way-hash service.  CSNBOWH returns
      *    the 32-byte raw digest in WS-HASH-VALUE.  When running on
      *    AWS Mainframe Modernization the Java equivalent of CSNBOWH
      *    is loaded via STATIC CALL.
           CALL 'CSNBOWH' USING WS-RETURN-CODE
                                WS-REASON-CODE
                                WS-EXIT-DATA-LEN
                                WS-EXIT-DATA
                                WS-RULE-ARRAY-COUNT
                                WS-RULE-ARRAY
                                WS-TEXT-LENGTH
                                WS-INPUT-BUFFER
                                WS-CHAIN-VAR-LEN
                                WS-CHAIN-VAR
                                WS-HASH-LENGTH
                                WS-HASH-VALUE
                ON EXCEPTION
                   MOVE 16 TO LK-RETURN-CODE
                   GOBACK
           END-CALL

           IF WS-RETURN-CODE NOT = 0
               MOVE WS-RETURN-CODE TO LK-RETURN-CODE
               GOBACK
           END-IF

      *    Hex-encode the 32 raw bytes into the 64-byte output buffer.
           PERFORM VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > 32
               MOVE WS-HASH-VALUE (WS-IDX:1) TO WS-BYTE-CHAR
               COMPUTE WS-BYTE-VALUE = FUNCTION ORD(WS-BYTE-CHAR) - 1
               COMPUTE WS-NIBBLE = FUNCTION INTEGER(WS-BYTE-VALUE / 16)
               COMPUTE WS-IDX-OUT = (WS-IDX - 1) * 2 + 1
               MOVE WS-HEX-DIGITS (WS-NIBBLE + 1:1)
                                            TO LK-HASH-OUT (WS-IDX-OUT:1)
               COMPUTE WS-NIBBLE = WS-BYTE-VALUE - (WS-NIBBLE * 16)
               COMPUTE WS-IDX-OUT = WS-IDX-OUT + 1
               MOVE WS-HEX-DIGITS (WS-NIBBLE + 1:1)
                                            TO LK-HASH-OUT (WS-IDX-OUT:1)
           END-PERFORM

           GOBACK.

       END PROGRAM CHASHPW.
