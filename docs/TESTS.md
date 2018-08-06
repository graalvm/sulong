# Sulong test cases

Sulong is tested using both self-maintained testsuites and select tests
from external suites. You can run all available testsuites using `mx gate`.
Please note that this command aborts as soon as one testsuite has failed.

## Testsuites

| Tag           | Class name          | Description                             |
|---------------|---------------------|-----------------------------------------|
| sulong        | SulongSuite         | Sulong's internal tests                 |
| interop       | LLVMInteropTest     | Truffle Language interoperability tests |
| debug         | LLVMDebugTest       | Debug support test suite                |
| llvm          | LLVMSuite           | LLVM 3.2 test suite                     |
| parser        | ParserTortureSuite  | Parser test using GCC suite             |
| nwcc          | NWCCSuite           | Test suite of the NWCC compiler v0.8.3  |
| assembly      | InlineAssemblyTest  | Inline assembler tests                  |
| gcc_c         | GCCSuite            | GCC 5.2 test suite (C tests)            |
| gcc_cpp       | GCCSuite            | GCC 5.2 test suite (C++ tests)          |
| gcc_fortran   | GCCSuite            | GCC 5.2 test suite (Fortran tests)      |
| args          | MainArgsTest        | Tests main args passing                 |
| benchmarks    | ShootoutsSuite      | Language Benchmark game tests           |
| vaargs        | VAArgsTest          | Varargs tests                           |
| pipe          | CaptureOutputTest   | Test output capturing                   |
| callback      | CallbackTest        | Test calling native functions           |
| type          | -                   | Test floating point arithmetic          |

The test cases consist of LLVM IR, C, C++, Fortran and Rust files. While
Sulong's Truffle LLVM IR interpreter can directly execute the LLVM IR
files it uses a selection from amongst the tools Clang, GCC and rustc to compile the other source files to LLVM IR
before executing them.

### Testgate

You can run specific testsuites by invoking `mx gate --tags [tags]`. For Sulong's
external test suites this command will download all neccessary files to
`<Sulong base dir>/tests` and compile them using Clang. The compiled files are
stored in `<Sulong base dir>/cache/tests`. Sulong's internal testsuites are compiled
together with Sulong. To recompile them one needs to do a complete rebuild of Sulong
using `mx clean && mx build` or `mx build -c`. You can find the sources in
`<Sulong base dir>/tests/com.oracle.truffle.llvm.tests.*` and the compiled files
in `<Sulong base dir>/mxbuild//sulong-test-suites`.

For easier use there are also some compound tags to execute smaller testsuites together.

| Tag          | Contained tags                                               |
|--------------|--------------------------------------------------------------|
| sulongBasic  | sulong, interop, debug                                       |
| sulongMisc   | benchmarks, type, pipe, assembly, args, callback, vaargs     |

The full `mx gate` command also performs various code quality checks.

| Tag          | Description                                                  |
|--------------|--------------------------------------------------------------|
| fullbuild    | Run Findbugs and create a clean build of Sulong using ECJ    |
| style        | Use various static anylsis tools to ensure code quality      |

#### Options

In order to pass polyglot options to Sulong one needs to specify them as JVM
arguments since `mx gate` does not support passing them directly.

    mx -A-Dpolyglot.llvm.<option> gate

### Unittests

The testsuites can also be executed using `mx unittest <classname>`. This
command expects the selected testsuites to have already been compiled using either
`mx build` for the internal testsuites or `mx gate` for the external ones.

`mx unittest` also supports running only selected tests of a specific suite. For
example, `test[c/max-unsigned-short-to-float-cast.c]` is part of the SulongSuite.
You can run only this test using
`mx unittest SulongSuite#test[c/max-unsigned-short-to-float-cast.c]`.

#### Options

For some testsuites it is necessary to specify required libraries, e.g. `libgfortran.so.3`
which is needed by the fortan tests of the `gcc_fortran` testsuite, manually using the
`polyglot.llvm.libraries` option. Since the `gate` command also uses `unittest`
to actually run the compiled tests you can find the full command it uses when running
`mx` with the `-v` option.

Another useful option to `unittest` is `--very-verbose`. This always prints the
test's name to the screen before it is started which can be a great tool to
identify tests are stuck.

Options need to be specified before the selected tests.

    mx <mx options> unittest <unittest/polyglot options> <tests>

### Debugging

To attach a debugger to Sulong tests, run `mx` with the `-d` argument, e.g.
`mx -d unittest SulongSuite` or `mx -d gate --tags sulong`.

It is also possible to trace Sulong's execution of the LLVM IR by using the
`-Dpolyglot.llvm.debug=true` option. This will print the value name
and the corresponding basic block number of each assignment as Sulong executes
the LLVM IR.

## Fortran

Some of our tests are Fortran files. Make sure you have GCC, G++, and GFortran
in version 4.5, 4.6 or 4.7 available.

On the Mac you can use Homebrew:

    brew tap homebrew/versions
    brew install gcc46 --with-fortran
    brew link --force gmp4

For the Fortran tests you also need to provide
[DragonEgg](http://dragonegg.llvm.org/) 3.2 and Clang 3.2.

[DragonEgg](http://dragonegg.llvm.org/) is a GCC plugin with which we
can use GCC to compile a source language to LLVM IR. Sulong uses
DragonEgg in its test cases to compile Fortran files to LLVM IR.
Sulong also uses DragonEgg for the C/C++ test cases besides Clang to get
additional "free" test cases for a given C/C++ file. DragonEgg requires
a GCC in the aforementioned versions.

- Sulong expects to find Clang 3.2 in `$DRAGONEGG_LLVM/bin`
- Sulong expects to find GCC 4.5, 4.6 or 4.7 in `$DRAGONEGG_GCC/bin`
- Sulong expects to find `dragonegg.so` under `$DRAGONEGG` or in `$DRAGONEGG_GCC/lib`

On some versions of Mac OS X, `gcc46` may fail to install with a segmentation
fault. You can find more details and suggestions on how to fix this here.

However you install GCC on the Mac, you may then need to manually link the
gcc libraries we use into a location where they can be found, as
DYLD_LIBRARY_PATH cannot normally be set on the Mac.

    ln -s /usr/local/Cellar/gcc46/4.6.4/lib/gcc/4.6/libgfortran.3.dylib /usr/local/lib

## Rust

Some of our tests are Rust files. Make sure you have a recent version of the Rust toolchain installer `rustup` available. When doing a complete rebuild of Sulong, an attempt will be made to compile the Rust test files using the rustc of the currently active toolchain (default: specified in the `rust-toolchain` file). Note that this may automatically install a missing Rust toolchain if the Rust version specified in the `rust-toolchain` file is unavailable.

For unittests Sulong automatically links the Rust standard library of the currently active toolchain. 

## Reference output

For most test cases, Sulong obtains the reference output (return value and/or
process output) by compiling the source file (or its LLVM IR version)
to machine code. An exception is the LLVM test suite, which offers
`.reference_output` files that we use as reference output.

## Configuration files

Some of the test suites have configuration files that include test cases
contained in a directory of that test suite. Sulong uses these configuration
files (ending with `.include`) mainly for external test suites, for which
not all test cases are supported, to select only a subset of these tests.
The table below shows in `config` which test suites use such configuration
files.

## Test case discovery

The test suites which have configuration files usually offer an option
to execute all test cases that are not included in the `.include` files,
in order to find newly supported test cases. To
exclude files from this discovery, there are also `.exclude` files in
the suite configuration, which are useful for test cases that crash the
JVM process or which will not be supported by Sulong in the near future.
