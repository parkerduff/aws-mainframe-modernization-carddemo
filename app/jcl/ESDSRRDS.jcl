//ESDSRRDS JOB 'DEF ESDS RRDS  ',REGION=8M,CLASS=A,
//      MSGCLASS=H,NOTIFY=&SYSUID
//******************************************************************
//* Copyright Amazon.com, Inc. or its affiliates.
//* All Rights Reserved.
//*
//* Licensed under the Apache License, Version 2.0 (the "License").
//* You may not use this file except in compliance with the License.
//* You may obtain a copy of the License at
//*
//*    http://www.apache.org/licenses/LICENSE-2.0
//*
//* Unless required by applicable law or agreed to in writing,
//* software distributed under the License is distributed on an
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
//* either express or implied. See the License for the specific
//* language governing permissions and limitations under the License
//******************************************************************
//*
//*-------------------------------------------------------------------*
//* SECURITY NOTE:
//*   The user-security records below contain SHA-256 hashed
//*   passwords and per-user salts; plaintext passwords are NEVER
//*   stored in the security file.  The hashes shown here are the
//*   demo-only hashes of the well-known password "PASSWORD" and are
//*   intended for development sandboxes only.
//*
//*   For production deployments use the bootstrap script
//*       db/migrate/init_user_security.sh
//*   which reads admin and user passwords from AWS Secrets Manager
//*   (or any compatible secret store) and writes the hashed records
//*   to either the modern user_security relational table or this
//*   VSAM file.  See config/secrets-config.md for details.
//*
//*   Record layout: copybook CSUSR01Y (record length = 160)
//*       USR-ID(8) FNAME(20) LNAME(20) LEGACY-PWD(8 - now blank)
//*       TYPE(1) FILLER(23) PWD-HASH(64) PWD-SALT(16)
//*-------------------------------------------------------------------*
//*
//*-------------------------------------------------------------------*
//* PRE DELETE STEP
//*-------------------------------------------------------------------*
//*
//PREDEL  EXEC PGM=IEFBR14
//*
//DD01     DD DSN=AWS.M2.CARDDEMO.ESDSRRDS.PS,
//            DISP=(MOD,DELETE,DELETE)
//*
//*-------------------------------------------------------------------*
//* CREATE USER SECURITY FILE (PS) FROM IN-STREAM DATA
//* Record length is 160 to accommodate SHA-256 hash and per-user salt.
//*-------------------------------------------------------------------*
//*
//STEP01  EXEC PGM=IEBGENER
//*
//SYSUT1   DD DATA,DLM=$$
ADMIN001MARGARET            GOLD                        A                       fb5b922aabd7e184674c6d0045d446d00d52d58c926f689a4942be715bcb851aADMIN001SALT0001
ADMIN002RUSSELL             RUSSELL                     A                       ef6bf27c35606918b017fcec4741eb93c57e0c40da340b7658ef18772338083cADMIN002SALT0002
ADMIN003RAYMOND             WHITMORE                    A                       1c8c9e868aeaae761d3892816544ee0eb5f4288150c28ce6f9d45f24472b9ba1ADMIN003SALT0003
ADMIN004EMMANUEL            CASGRAIN                    A                       0ed7ecfcc61872075a8a9e17938fc831fa8c1da4450b318f8792807a5c3b1e1fADMIN004SALT0004
ADMIN005GRANVILLE           LACHAPELLE                  A                       746c9ee622f399e4ad0bf128596e914d45e409e99db715e83d4a65b7a70afb07ADMIN005SALT0005
USER0001LAWRENCE            THOMAS                      U                       0e2c804c27c128ea323c6353325167c969cdc6e892c790beec8cfab0d288d398USER0001SALT0001
USER0002AJITH               KUMAR                       U                       8c4397ee205f44a726f179f8fb4c7e90a58300262299cba97cb49ec84cb6e691USER0002SALT0002
USER0003LAURITZ             ALME                        U                       7e53c57a352c5685fbb90578bc0a15f6270ec45e9524a3854b00db3bf2f930dbUSER0003SALT0003
USER0004AVERARDO            MAZZI                       U                       08468e3c5fb153ba40d132903b4b90d78a35e5ebe3d128a152ec1e78ec1fe3b0USER0004SALT0004
USER0005LEE                 TING                        U                       80bb3fafb9653d354bc958905f52879771412d3c892fb6cebadd4e00eca64e1cUSER0005SALT0005
$$
//SYSUT2   DD DSN=AWS.M2.CARDDEMO.ESDSRRDS.PS,
//            DISP=(NEW,CATLG,DELETE),
//            DCB=(LRECL=160,RECFM=FB,DSORG=PS,BLKSIZE=0),
//            UNIT=SYSAD,SPACE=(TRK,(10,5),RLSE)
//*
//SYSPRINT DD SYSOUT=*
//SYSIN    DD DUMMY
//*
//*-------------------------------------------------------------------*
//* DEFINE VSAM FILE FOR USER SECURITY (ESDS)
//*-------------------------------------------------------------------*
//*
//STEP02  EXEC PGM=IDCAMS
//*
//SYSPRINT DD  SYSOUT=*
//SYSIN    DD  *
 DELETE                  AWS.M2.CARDDEMO.USRSEC.VSAM.ESDS
 SET       MAXCC = 0
 DEFINE    CLUSTER (NAME(AWS.M2.CARDDEMO.USRSEC.VSAM.ESDS)    -
                    RECORDSIZE(160,160)                       -
                    REUSE                                     -
                    NONINDEXED                                -
                    TRACKS(45,15)                             -
                    FREESPACE(10,15)                          -
                    CISZ(8192))                               -
           DATA    (NAME(AWS.M2.CARDDEMO.USRSEC.VSAM.ESDS.DAT))
/*
//*
//*-------------------------------------------------------------------*
//* COPY USER SECURITY DATA FROM PS TO VSAM FILE(ESDS)
//*-------------------------------------------------------------------*
//*
//STEP03  EXEC PGM=IDCAMS
//*
//IN       DD  DSN=AWS.M2.CARDDEMO.ESDSRRDS.PS,DISP=SHR
//OUT      DD  DSN=AWS.M2.CARDDEMO.USRSEC.VSAM.ESDS,DISP=SHR
//SYSOUT   DD  SYSOUT=*
//SYSPRINT DD  SYSOUT=*
//SYSIN    DD  *
  REPRO INFILE(IN) OUTFILE(OUT)
/*
//*
//*-------------------------------------------------------------------*
//* DEFINE VSAM FILE FOR USER SECURITY (RRDS)
//*-------------------------------------------------------------------*
//*
//STEP04  EXEC PGM=IDCAMS
//*
//SYSPRINT DD  SYSOUT=*
//SYSIN    DD  *
 DELETE                  AWS.M2.CARDDEMO.USRSEC.VSAM.RRDS
 SET       MAXCC = 0
 DEFINE    CLUSTER (NAME(AWS.M2.CARDDEMO.USRSEC.VSAM.RRDS)    -
                    RECORDSIZE(160,160)                       -
                    REUSE                                     -
                    NUMBERED                                  -
                    TRACKS(45,15)                             -
                    FREESPACE(10,15)                          -
                    CISZ(8192))                               -
           DATA    (NAME(AWS.M2.CARDDEMO.USRSEC.VSAM.RRDS.DAT))
/*
//*
//*-------------------------------------------------------------------*
//* COPY USER SECURITY DATA FROM PS TO VSAM FILE(ESDS)
//*-------------------------------------------------------------------*
//*
//STEP05  EXEC PGM=IDCAMS
//*
//IN       DD  DSN=AWS.M2.CARDDEMO.ESDSRRDS.PS,DISP=SHR
//OUT      DD  DSN=AWS.M2.CARDDEMO.USRSEC.VSAM.RRDS,DISP=SHR
//SYSOUT   DD  SYSOUT=*
//SYSPRINT DD  SYSOUT=*
//SYSIN    DD  *
  REPRO INFILE(IN) OUTFILE(OUT)
/*
//
//* Ver: CardDemo_v1.0-15-g27d6c6f-68 Date: 2022-07-19 23:23:04 CDT
//*
