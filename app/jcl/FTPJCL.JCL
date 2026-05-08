//FTPJCLS  JOB 'FTP JCL',CLASS=A,MSGCLASS=H,MSGLEVEL=(1,1),             JOB01598
//             REGION=0M,NOTIFY=&SYSUID,TIME=1440
//*
//*
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
//*********************************************************************
//* FTP JOB TO RECEIVE A FILE
//*
//* SECURITY NOTE:
//*   FTP credentials must NEVER be stored in plaintext inside source-
//*   controlled JCL.  Connection details are sourced from the symbolic
//*   parameters &FTPHOST, &FTPUSER and &FTPPASS which are populated by
//*   the scheduler / job submission tooling from a secure credential
//*   store (AWS Secrets Manager / SSM Parameter Store, HashiCorp Vault,
//*   IBM RACF or your enterprise PAM solution).
//*
//*   See config/secrets-config.md for details on how the symbolic
//*   parameters are resolved at submission time.
//*
//*   Symbolic parameters (override on JOB card or via job library):
//*     FTPHOST  - target FTP server hostname or IP
//*     FTPUSER  - service account user id
//*     FTPPASS  - reference to a secret value, NOT the value itself
//*********************************************************************
//STEP1 EXEC PGM=FTP,REGION=2048K,
//             PARM='(EXIT TIMEOUT 20'
//*
//* The credential lines below are intentionally placeholders.  At
//* submission time the scheduler must materialise them from the secure
//* credential store before the job is enqueued.  See:
//*     config/secrets-config.md
//*     infrastructure/cloudformation/secrets.yaml
//*
//SYSIN DD *
 &FTPHOST
 &FTPUSER
 &FTPPASS
 ASCII
 pwd
 dir
 cd /ftpfolder
 PUT 'AWS.M2.CARDEMO.FTP.TEST' welcome.txt
 QUIT
//*
