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
      ******************************************************************
      * SECURITY NOTE:
      *   The user-security record now stores a SHA-256 password hash
      *   plus a per-user salt instead of the plaintext password.
      *   The legacy SEC-USR-PWD field is retained for binary
      *   compatibility with already-deployed images but is BLANK in
      *   newly-bootstrapped records and MUST NOT be referenced for
      *   authentication.  Use SEC-USR-PWD-HASH and SEC-USR-PWD-SALT
      *   together with subprogram CHASHPW (SHA-256) to verify a
      *   supplied password.  See COSGN00C.cbl, READ-USER-SEC-FILE
      *   and config/secrets-config.md for details.
      *   Record length: 160 bytes (was 80).
      ******************************************************************
       01 SEC-USER-DATA.
         05 SEC-USR-ID                 PIC X(08).
         05 SEC-USR-FNAME              PIC X(20).
         05 SEC-USR-LNAME              PIC X(20).
         05 SEC-USR-PWD                PIC X(08).
         05 SEC-USR-TYPE               PIC X(01).
         05 SEC-USR-FILLER             PIC X(23).
         05 SEC-USR-PWD-HASH           PIC X(64).
         05 SEC-USR-PWD-SALT           PIC X(16).
      *
      * Ver: CardDemo_v1.0-15-g27d6c6f-68 Date: 2022-07-19 23:15:59 CDT
      *
