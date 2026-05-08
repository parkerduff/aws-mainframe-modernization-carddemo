# Contributing Guidelines

Thank you for your interest in contributing to our project. Whether it's a bug report, new feature, correction, or additional
documentation, we greatly value feedback and contributions from our community.

Please read through this document before submitting any issues or pull requests to ensure we have all the necessary
information to effectively respond to your bug report or contribution.


## Reporting Bugs/Feature Requests

We welcome you to use the GitHub issue tracker to report bugs or suggest features.

When filing an issue, please check existing open, or recently closed, issues to make sure somebody else hasn't already
reported the issue. Please try to include as much information as you can. Details like these are incredibly useful:

* A reproducible test case or series of steps
* The version of our code being used
* Any modifications you've made relevant to the bug
* Anything unusual about your environment or deployment


## Contributing via Pull Requests
Contributions via pull requests are much appreciated. Before sending us a pull request, please ensure that:

1. You are working against the latest source on the *main* branch.
2. You check existing open, and recently merged, pull requests to make sure someone else hasn't addressed the problem already.
3. You open an issue to discuss any significant work - we would hate for your time to be wasted.

To send us a pull request, please:

1. Fork the repository.
2. Modify the source; please focus on the specific change you are contributing. If you also reformat all the code, it will be hard for us to focus on your change.
3. Ensure local tests pass.
4. Commit to your fork using clear commit messages.
5. Send us a pull request, answering any default questions in the pull request interface.
6. Pay attention to any automated CI failures reported in the pull request, and stay involved in the conversation.

GitHub provides additional document on [forking a repository](https://help.github.com/articles/fork-a-repo/) and
[creating a pull request](https://help.github.com/articles/creating-a-pull-request/).


## Finding contributions to work on
Looking at the existing issues is a great way to find something to contribute on. As our projects, by default, use the default GitHub issue labels (enhancement/bug/duplicate/help wanted/invalid/question/wontfix), looking at any 'help wanted' issues is a great place to start.


## Code of Conduct
This project has adopted the [Amazon Open Source Code of Conduct](https://aws.github.io/code-of-conduct).
For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq) or contact
opensource-codeofconduct@amazon.com with any additional questions or comments.


## Security issue notifications
If you discover a potential security issue in this project we ask that you notify AWS/Amazon Security via our [vulnerability reporting page](http://aws.amazon.com/security/vulnerability-reporting/). Please do **not** create a public github issue.


## Licensing

See the [LICENSE](LICENSE) file for our project's licensing. We will ask you to confirm the licensing of your contribution.


## Coding standards

These standards govern the modernised code base.  They apply to every
new program, copybook and JCL member added under this repository.

### COBOL

* **Free-form, columns 1-72.**  All new programs must compile with both
  the IBM Enterprise COBOL and the AWS Mainframe Modernization Micro
  Focus runtime, so avoid features that are unique to a single dialect.
* **Naming.**
  * Programs: 8 characters, all uppercase, prefix with the subsystem
    (`CO` for online, `CB` for batch, `DB` for data-access).
  * Paragraphs: numeric prefix that increases monotonically through
    the program.  Use `0000-MAIN-DRIVER`, `1000-...`, `2000-...` for
    the major phases and `9999-ABEND-PROGRAM` for the abend handler.
    Do **not** mix `9999-ABEND-PROGRAM` and `Z-ABEND-PROGRAM` in the
    same program.
  * Working storage: dash-separated, prefix indicates scope - `WS-` for
    working storage, `LK-` for linkage, `LS-` for local storage,
    `DAL-` for the data-access-layer copybook.
  * Copybooks: 8 characters, all uppercase, suffix `Y` (e.g.
    `DBACCESY`).  The `Y` differentiates copybook members from
    programs in scheduler / control libraries.
* **Headers.**  Every program must start with the standard 7-line
  banner shown in the existing `CO*.cbl` programs (program id,
  application, type, function, last change date, author, copyright).
* **Error handling.**
  * Always check `EIBRESP` (online) or the SQLCA (batch) after every
    EXEC CICS / EXEC SQL block.
  * Centralise the abend logic in `9999-ABEND-PROGRAM` and call it via
    `PERFORM` rather than inline `EXEC CICS ABEND`.
  * Never silently swallow `NOTFND` - either set an error message and
    return to the caller, or `PERFORM 9999-ABEND-PROGRAM`.
* **Data access.**  New code must call the shared data-access layer
  (`app/cbl/DBACCESS.cbl`) instead of issuing `EXEC CICS READ FILE` or
  raw `EXEC SQL` statements.  This keeps the strangler-fig migration
  reversible.

### JCL

* JOB cards must use symbolic parameters or `JCLLIB ORDER` for any
  environment-specific value.  Do **not** embed hostnames, IP
  addresses or credentials in the source-controlled JCL.
* All members must end with a newline and stay within 80 columns.
* Comments use the `//*` form and document the purpose, expected
  inputs/outputs and any operational dependencies.

### SQL

* DDL lives in `db/schema.sql`; do not scatter `CREATE TABLE`
  statements across migration scripts.
* Column names mirror the COBOL field names from the source copybook
  (lower-cased, dash to underscore).  This is what makes
  `app/cbl/DBACCESS.cbl` regression-testable.
* Every table has a primary key.  Foreign keys are defined in the same
  file as the child table.

### Python (migration & test tooling)

* Targets Python 3.11+.  Run `pytest tests/unit` before pushing.
* Type hints required on all public functions.
* Logging via the stdlib `logging` module, not `print()`, except in
  the `__main__` driver scripts.

### Secrets

* No plaintext secrets in any committed file.  See
  `config/secrets-config.md` for the supported secret stores.
* Adding a literal that even *looks* like a credential will trip the
  `tests/lint/scan_secrets.py` linter; either remove it or add a
  scoped allow-list entry.
