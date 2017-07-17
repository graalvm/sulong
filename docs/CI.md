# Continuous Integration (CI)

Before a commit to Sulong is merged, it first has to pass all our CI tests.
We have two checks in place:

1. The OCA check: In order to contribute to Sulong, [each contributor has
to sign and submit the Oracle Contributor Agreement (OCA)](CONTRIBUTING.md).
The OCA check confirms that a contributor has signed the OCA.

2. The Travis gate: We run a test gate in which we execute our test cases
and static analysis tools.

3. Internal tests: In addition to Travis CI we also use an additional, private,
test gate in which we run further tests on additional clang versions.

## The Travis Gate

Travis is an open-source CI service. We use this service to execute our
test gate. The gate checks are defined in the
[`.travis.yml` configuration file](/.travis.yml).

The gate performs the following tasks:

1. a check for the warning-free compilation of all Java sources with
   `javac` and `ECJ`: To check the warning-free compilation locally, use
   `mx build` with the `JDT` environment variable set to an `ECJ` jar
   file to compile with `ECJ` and otherwise with `javac`.
2. the execution of all [test suites](docs/TESTS.md): Use
    `mx tests && mx unittest Sulongsuite` to execute the test cases.
3. confirming that Java files are correctly formatted using `eclipseformat`
   (which is normally executed when saving a Java file in Eclipse):
   Use `mx eclipseformat --primary` with the `ECLIPSE_EXE` environment
   variable set to eclipse.
4. confirming that there are no Java style violations using `checkstyle`
   (the configurations are defined per-project in the `checkstyle` attribute
   of the [suite.py](/mx.sulong/suite.py) file). Use `mx checkstyle` to
   execute the check locally.
5. confirming that there are no Python format or style violations using
   `pylint`. Use `mx pylint` to execute the check locally.
6. confirming that C and C++ files are correctly formatted using `clang-format`:
   Use `mx check clangformatcheck` to execute the check locally.
7. checking that the mx configurations and project names are in the expected way
   using mx's `checkoverlap` and `canonicalizeprojects`: Use `mx checkoverlap`
   and `mx canonicalizeprojects` to execute the mx checks locally.
8. checking that the mx and Travis files do not contain `http://` links
   due to security reasons: Use `mx check httpcheck` to execute the check locally.
10. checking that the Markdown (`.md`) files contain no style violations.
   Use `mx check mdl` to execute the check locally.

