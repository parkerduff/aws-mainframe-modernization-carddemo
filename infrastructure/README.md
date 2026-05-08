# CardDemo Modernised Infrastructure

CloudFormation templates for the AWS deployment target of the
modernised CardDemo stack.  Each template is self-contained and can
be deployed independently using `aws cloudformation deploy` (the CI
pipeline does this in the `deploy` job).

| Template                              | Purpose                                                                 |
|---------------------------------------|-------------------------------------------------------------------------|
| `cloudformation/networking.yaml`      | VPC, public/private subnets, security groups for the database & M2 env |
| `cloudformation/database.yaml`        | RDS for PostgreSQL (Aurora-compatible) for the relational schema        |
| `cloudformation/secrets.yaml`         | Secrets Manager entries for FTP, DB, bootstrap users                    |
| `cloudformation/m2-environment.yaml`  | AWS Mainframe Modernization environment + S3 staging buckets            |
| `cloudformation/iam.yaml`             | CI/CD deploy role with the minimal required policy set                  |

## Bring up the stack

```bash
aws cloudformation deploy \
    --stack-name carddemo-network \
    --template-file infrastructure/cloudformation/networking.yaml \
    --capabilities CAPABILITY_NAMED_IAM

aws cloudformation deploy \
    --stack-name carddemo-database \
    --template-file infrastructure/cloudformation/database.yaml \
    --parameter-overrides \
        DBPassword=$(aws secretsmanager get-secret-value \
            --secret-id /carddemo/dev/db/password \
            --query SecretString --output text) \
    --capabilities CAPABILITY_NAMED_IAM
...
```

The CI pipeline performs the deployment automatically when a maintainer
manually triggers the `Deploy (AWS Mainframe Modernization)` workflow
job on the `main` branch.
