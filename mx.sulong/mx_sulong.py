#
# Copyright (c) 2016, 2018, Oracle and/or its affiliates.
#
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification, are
# permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice, this list of
# conditions and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright notice, this list of
# conditions and the following disclaimer in the documentation and/or other materials provided
# with the distribution.
#
# 3. Neither the name of the copyright holder nor the names of its contributors may be used to
# endorse or promote products derived from this software without specific prior written
# permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
# OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
# AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
# OF THE POSSIBILITY OF SUCH DAMAGE.
#
import tarfile
import os
from os.path import join
import shutil
import subprocess
import glob
from argparse import ArgumentParser

import mx
import mx_subst
import mx_sdk
import re
import mx_benchmark
import mx_sulong_benchmarks
import mx_unittest

from mx_unittest import add_config_participant
from mx_gate import Task, add_gate_runner

import mx_testsuites

# re-export SulongTestSuite class so it can be used from suite.py
from mx_testsuites import SulongTestSuite #pylint: disable=unused-import

_suite = mx.suite('sulong')
_mx = join(_suite.dir, "mx.sulong")
_root = join(_suite.dir, "projects")
_testDir = join(_suite.dir, "tests")
_toolDir = join(_suite.dir, "cache", "tools")
_clangPath = join(_toolDir, "llvm", "bin", "clang")



# the supported GCC versions (see dragonegg.llvm.org)
supportedGCCVersions = [
    '4.6',
    '4.5',
    '4.7'
]

# the LLVM versions supported by the current bitcode parser that bases on the textual format
# sorted by priority in descending order (highest priority on top)
supportedLLVMVersions = [
    '3.2',
    '3.3',
    '3.8',
    '3.9',
    '4.0',
    '5.0',
    '6.0',
]

# the basic LLVM dependencies for running the test cases and executing the mx commands
basicLLVMDependencies = [
    'clang',
    'clang++',
    'rustc',
    'opt',
    'llc',
    'llvm-as'
]

# the file paths that we want to check with clang-format
clangFormatCheckPaths = [
    join(_suite.dir, "include"),
    join(_root, "com.oracle.truffle.llvm.libraries.bitcode", "src"),
    join(_root, "com.oracle.truffle.llvm.libraries.bitcode", "include"),
    join(_root, "com.oracle.truffle.llvm.pipe.native", "src"),
    join(_testDir, "com.oracle.truffle.llvm.tests.sulong"),
    join(_testDir, "com.oracle.truffle.llvm.tests.sulongcpp"),
    join(_testDir, "interoptests"),
    join(_testDir, "inlineassemblytests"),
    join(_testDir, "other")
]

# the clang-format versions that can be used for formatting the test case C and C++ files
clangFormatVersions = [
    '3.8',
    '3.9',
    '4.0',
]

# the file paths that we want to check with rustfmt
rustfmtCheckPaths = [
    join(_testDir, "com.oracle.truffle.llvm.tests.sulongrust")
]

def _unittest_config_participant(config):
    (vmArgs, mainClass, mainClassArgs) = config
    testSuitePath = mx_subst.path_substitutions.substitute('<path:SULONG_TEST_SUITES>')
    libs = [mx_subst.path_substitutions.substitute('<path:SULONG_TEST_NATIVE>/<lib:sulongtest>')] + getUnittestLibraryDependencies(testSuitePath)
    vmArgs = getCommonOptions(True, libs) + vmArgs
    return (vmArgs, mainClass, mainClassArgs)

add_config_participant(_unittest_config_participant)

def getUnittestLibraryDependencies(libs_files_path):
    libs = []
    for libs_file in glob.glob(os.path.join(libs_files_path, '*', 'libs')):
        with open(libs_file, 'r') as l:
            libs += l.readline().split()
    return [mx_subst.path_substitutions.substitute(lib) for lib in libs]

class TemporaryEnv(object):
    def __init__(self, key, value):
        self.key = key
        self.value = value
        self.old_value = None

    def __enter__(self):
        self.old_value = os.environ.get(self.key)
        os.environ[self.key] = self.value

    def __exit__(self, ex_type, value, traceback):
        if self.old_value is None:
            del os.environ[self.key]
        else:
            os.environ[self.key] = self.old_value

def _sulong_gate_runner(args, tasks):
    with TemporaryEnv('LC_ALL', 'C'):
        with Task('CheckCopyright', tasks, tags=['style']) as t:
            if t:
                if mx.checkcopyrights(['--primary']) != 0:
                    t.abort('Copyright errors found. Please run "mx checkcopyrights --primary -- --fix" to fix them.')
        with Task('ClangFormat', tasks, tags=['style', 'clangformat']) as t:
            if t: clangformatcheck()
        with Task('Rustfmt', tasks, tags=['style', 'rustfmt']) as t:
            if t: rustfmtcheck()
        with Task('TestBenchmarks', tasks, tags=['benchmarks', 'sulongMisc']) as t:
            if t: mx_testsuites.runSuite('shootout')
        with Task('TestTypes', tasks, tags=['type', 'sulongMisc']) as t:
            if t: mx_testsuites.runSuite('type')
        with Task('TestPipe', tasks, tags=['pipe', 'sulongMisc']) as t:
            if t: mx_testsuites.runSuite('pipe')
        with Task('TestLLVM', tasks, tags=['llvm']) as t:
            if t: mx_testsuites.runSuite('llvm')
        with Task('TestNWCC', tasks, tags=['nwcc']) as t:
            if t: mx_testsuites.runSuite('nwcc')
        with Task('TestGCCParserTorture', tasks, tags=['parser']) as t:
            if t: mx_testsuites.runSuite('parserTorture')
        with Task('TestGCC_C', tasks, tags=['gcc_c']) as t:
            if t: mx_testsuites.runSuite('gcc_c')
        with Task('TestGCC_CPP', tasks, tags=['gcc_cpp']) as t:
            if t: mx_testsuites.runSuite('gcc_cpp')
        with Task('TestGCC_Fortran', tasks, tags=['gcc_fortran']) as t:
            if t: mx_testsuites.runSuite('gcc_fortran')
        with Task("TestSulong", tasks, tags=['sulong', 'sulongBasic']) as t:
            if t: mx_unittest.unittest(['SulongSuite'])
        with Task("TestInterop", tasks, tags=['interop', 'sulongBasic']) as t:
            if t: mx_unittest.unittest(['com.oracle.truffle.llvm.test.interop'])
        with Task("TestDebug", tasks, tags=['debug', 'sulongBasic']) as t:
            if t: mx_unittest.unittest(['LLVMDebugTest'])
        with Task('TestAssembly', tasks, tags=['assembly', 'sulongMisc']) as t:
            if t: mx_testsuites.runSuite('assembly')
        with Task('TestArgs', tasks, tags=['args', 'sulongMisc']) as t:
            if t: mx_testsuites.runSuite('args')
        with Task('TestCallback', tasks, tags=['callback', 'sulongMisc']) as t:
            if t: mx_testsuites.runSuite('callback')
        with Task('TestVarargs', tasks, tags=['vaargs', 'sulongMisc']) as t:
            if t: mx_testsuites.runSuite('vaargs')

add_gate_runner(_suite, _sulong_gate_runner)


def testLLVMImage(image, imageArgs=None, testFilter=None, libPath=True, test=None, unittestArgs=None):
    """runs the SulongSuite tests on an AOT compiled lli image"""
    args = ['-Dsulongtest.testAOTImage=' + image]
    aotArgs = []
    if libPath:
        aotArgs += [mx_subst.path_substitutions.substitute('-Dllvm.home=<path:SULONG_LIBS>')]
    libs = getUnittestLibraryDependencies(mx_subst.path_substitutions.substitute('<path:SULONG_TEST_SUITES>'))
    if libs:
        aotArgs += ['--llvm.libraries=' + ':'.join(libs)]
    if imageArgs is not None:
        aotArgs += imageArgs
    if aotArgs:
        args += ['-Dsulongtest.testAOTArgs=' + ' '.join(aotArgs)]
    if testFilter is not None:
        args += ['-Dsulongtest.testFilter=' + testFilter]
    testName = 'SulongSuite'
    if test is not None:
        testName += '#test[' + test + ']'
    if unittestArgs is None:
        unittestArgs = []
    mx_unittest.unittest(args + [testName] + unittestArgs)

def _test_llvm_image(args):
    """run the SulongSuite tests on an AOT compiled lli image"""
    parser = ArgumentParser(prog='mx test-llvm-image', description='Run the SulongSuite tests on an AOT compiled LLVM image.',
            epilog='Additional arguments are forwarded to the LLVM image command.')
    parser.add_argument('--omit-library-path', action='store_false', dest='libPath', help='do not add standard library path to arguments')
    parser.add_argument('--test', action='store', dest='test', help='run a single test (default: run all)')
    parser.add_argument('--test-filter', action='store', dest='testFilter', help='filter test variants to execute')
    parser.add_argument('image', help='path to pre-built LLVM image', metavar='<image>')
    for testArg in ['--verbose', '--very-verbose', '--enable-timing', '--color']:
        parser.add_argument(testArg, action='append_const', dest='unittestArgs', const=testArg, help='forwarded to mx unittest')
    (args, imageArgs) = parser.parse_known_args(args)
    testLLVMImage(args.image, imageArgs=imageArgs, testFilter=args.testFilter, libPath=args.libPath, test=args.test, unittestArgs=args.unittestArgs)

# routine for AOT downstream tests
def runLLVMUnittests(unittest_runner):
    """runs the interop unit tests with a different unittest runner (e.g. AOT-based)"""
    langhome = mx_subst.path_substitutions.substitute('-Dllvm.home=<path:SULONG_LIBS>')
    libpath = mx_subst.path_substitutions.substitute('-Dpolyglot.llvm.libraryPath=<path:SULONG_TEST_NATIVE>')
    libs = mx_subst.path_substitutions.substitute('-Dpolyglot.llvm.libraries=<lib:sulongtest>')
    unittest_runner(unittest_args=['com.oracle.truffle.llvm.test.interop'], run_args=[langhome, libpath, libs])


def clangformatcheck(args=None):
    """ Performs a format check on the include/truffle.h file """
    for f in clangFormatCheckPaths:
        checkFiles(f, checkCFile, ['.c', '.cpp', '.h', '.hpp'])

def rustfmtcheck(args=None):
    """ Performs a format check on the Rust test files """
    if not checkRustComponent('rustfmt'):
        mx.warn("'rustfmt' is not available")
        return
    for f in rustfmtCheckPaths:
        checkFiles(f, checkRustFile, ['rs'])

def checkFiles(targetDir, fileChecker, exts):
    error = False
    for path, _, files in os.walk(targetDir):
        for f in files:
            if f.endswith(tuple(exts)):
                if not fileChecker(path + '/' + f):
                    error = True
    if error:
        mx.log_error("found formatting errors!")
        exit(-1)

def checkCFile(targetFile):
    """ Checks the formatting of a C file and returns True if the formatting is okay """
    clangFormat = findInstalledLLVMProgram('clang-format', clangFormatVersions)
    if clangFormat is None:
        exit("Unable to find 'clang-format' executable with one the supported versions '" + ", ".join(clangFormatVersions) + "'")
    formatCommand = [clangFormat, targetFile]
    formattedContent = subprocess.check_output(formatCommand).splitlines()
    with open(targetFile) as f:
        originalContent = f.read().splitlines()
    if not formattedContent == originalContent:
        # modify the file to the right format
        subprocess.check_output(formatCommand + ['-i'])
        mx.log('\n'.join(formattedContent))
        mx.log('\nmodified formatting in {0} to the format above'.format(targetFile))
        return False
    return True

def checkRustFile(targetFile):
    """ Checks the formatting of a Rust file and returns True if the formatting is okay """
    if not checkRustComponent('rustfmt'):
        exit("Unable to find 'rustfmt' executable")
    returncode_check = mx.run(['rustfmt', '--check', targetFile], nonZeroIsFatal=False)
    if returncode_check == 1:
        # formatted code differs from existing code or error occured; try to modify the file to the right format
        returncode_replace = mx.run(['rustfmt', targetFile], nonZeroIsFatal=False)
        if returncode_replace == 0:
            mx.log('modified formatting in {0}'.format(targetFile))
            return False
    elif returncode_check == 0:
        return True
    mx.log_error('encountered parsing errors or operational errors when trying to format {0}'.format(targetFile))
    return False

# platform dependent
def pullLLVMBinaries(args=None):
    """downloads the LLVM binaries"""
    toolDir = join(_toolDir, "llvm")
    mx.ensure_dir_exists(toolDir)
    osStr = mx.get_os()
    arch = mx.get_arch()
    if osStr == 'windows':
        mx.log_error('windows currently only supported with cygwin!')
        return
    elif osStr == 'linux':
        if arch == 'amd64':
            urls = ['https://lafo.ssw.uni-linz.ac.at/pub/sulong-deps/clang+llvm-3.2-x86_64-linux-ubuntu-12.04.tar.gz']
        else:
            urls = ['https://lafo.ssw.uni-linz.ac.at/pub/sulong-deps/clang+llvm-3.2-x86-linux-ubuntu-12.04.tar.gz']
    elif osStr == 'darwin':
        urls = ['https://lafo.ssw.uni-linz.ac.at/pub/sulong-deps/clang+llvm-3.2-x86_64-apple-darwin11.tar.gz']
    elif osStr == 'cygwin':
        urls = ['https://lafo.ssw.uni-linz.ac.at/pub/sulong-deps/clang+llvm-3.2-x86-mingw32-EXPERIMENTAL.tar.gz']
    else:
        mx.log_error("{0} {1} not supported!".format(osStr, arch))
    localPath = pullsuite(toolDir, urls)
    tar(localPath, toolDir, stripLevels=1)
    os.remove(localPath)

def dragonEggPath():
    if 'DRAGONEGG' in os.environ:
        return join(os.environ['DRAGONEGG'], mx.add_lib_suffix('dragonegg'))
    if 'DRAGONEGG_GCC' in os.environ:
        path = join(os.environ['DRAGONEGG_GCC'], 'lib', mx.add_lib_suffix('dragonegg'))
        if os.path.exists(path):
            return path
    return None

def dragonEgg(args=None):
    """executes GCC with dragonegg"""
    executeCommand = [getGCC(), "-fplugin=" + dragonEggPath(), '-fplugin-arg-dragonegg-emit-ir']
    return mx.run(executeCommand + args)

def dragonEggGFortran(args=None):
    """executes GCC Fortran with dragonegg"""
    executeCommand = [getGFortran(), "-fplugin=" + dragonEggPath(), '-fplugin-arg-dragonegg-emit-ir']
    return mx.run(executeCommand + args)

def dragonEggGPP(args=None):
    """executes G++ with dragonegg"""
    executeCommand = [getGPP(), "-fplugin=" + dragonEggPath(), '-fplugin-arg-dragonegg-emit-ir']
    return mx.run(executeCommand + args)

def checkRustComponent(componentName):
    """checks if a Rust component is available; may try to install the active toolchain if it is missing"""
    if (componentName == 'rustc' and os.environ.get('SULONG_USE_RUSTC', 'true') == 'false') or which(componentName) is None:
        return False

    component = subprocess.Popen([componentName, '--version'], stdout=subprocess.PIPE)
    component.communicate()
    return component.returncode == 0

def which(program, searchPath=None):
    def is_exe(fpath):
        return os.path.isfile(fpath) and os.access(fpath, os.X_OK)

    fpath, _ = os.path.split(program)
    if fpath:
        if is_exe(program):
            return program
    else:
        if searchPath is None:
            searchPath = os.environ["PATH"].split(os.pathsep)
        for path in searchPath:
            path = path.strip('"')
            exe_file = os.path.join(path, program)
            if is_exe(exe_file):
                return exe_file
    return None

def getCommand(envVariable):
    """gets an environment variable and checks that it is an executable program"""
    command = os.getenv(envVariable)
    if command is None:
        return None
    else:
        if which(command) is None:
            mx.abort(envVariable + '=' + command +' specifies an invalid command!')
        else:
            return command

def getGCC(optional=False):
    """tries to locate a gcc version suitable to execute Dragonegg"""
    specifiedGCC = getCommand('SULONG_GCC')
    if specifiedGCC is not None:
        return specifiedGCC
    return findGCCProgram('gcc', optional=optional)

def getGFortran(optional=False):
    """tries to locate a gfortran version suitable to execute Dragonegg"""
    specifiedGFortran = getCommand('SULONG_GFORTRAN')
    if specifiedGFortran is not None:
        return specifiedGFortran
    return findGCCProgram('gfortran', optional=optional)

def getGPP(optional=False):
    """tries to locate a g++ version suitable to execute Dragonegg"""
    specifiedCPP = getCommand('SULONG_GPP')
    if specifiedCPP is not None:
        return specifiedCPP
    return findGCCProgram('g++', optional=optional)

def findLLVMProgramForDragonegg(program):
    """tries to find a supported version of an installed LLVM program; if the program is not found it downloads the LLVM binaries and checks there"""
    installedProgram = findInstalledLLVMProgram(program, ['3.2', '3.3'])

    if installedProgram is None:
        if 'DRAGONEGG_LLVM' in os.environ:
            path = os.environ['DRAGONEGG_LLVM']
        else:
            if not os.path.exists(_clangPath):
                pullLLVMBinaries()
            path = os.path.join(_toolDir, 'llvm')
        return os.path.join(path, 'bin', program)
    else:
        return installedProgram

def tar(tarFile, currentDir, subDirInsideTar=None, stripLevels=None):
    with tarfile.open(tarFile) as tar:
        if subDirInsideTar is None:
            files = tar.getmembers()
        else:
            files = []
            for tarinfo in tar.getmembers():
                for curDir in subDirInsideTar:
                    if tarinfo.name.startswith(curDir):
                        files.append(tarinfo)
        tar.extractall(members=files, path=currentDir)
    if not stripLevels is None:
        if subDirInsideTar is None:
            implicitPathComponents = files[0].name.split(os.sep)[:stripLevels]
            implicitPath = ""
            for comp in implicitPathComponents:
                implicitPath += comp + os.sep
            implicitPath = implicitPath.rstrip('/')
            stripDirectoryList = [implicitPath]
        else:
            stripDirectoryList = subDirInsideTar
        for currentSubDir in stripDirectoryList:
            stripDir(currentDir, currentSubDir, stripLevels)
        toDelete = os.path.join(currentDir, files[0].name.split(os.sep)[0])
        shutil.rmtree(toDelete)

def stripDir(dirPath, dirToStrip, nrLevels):
    cleanedDirPath = dirToStrip.rstrip('/')
    pathComponents = cleanedDirPath.split(os.sep)[nrLevels:]
    strippedPath = ""
    for component in pathComponents:
        strippedPath += component + os.sep
    srcPath = os.path.join(dirPath, dirToStrip)
    destPath = os.path.join(dirPath, strippedPath)
    copytree(srcPath, destPath)

def copytree(src, dst, symlinks=False, ignore=None):
    if not os.path.exists(dst):
        os.makedirs(dst)
    for item in os.listdir(src):
        s = os.path.join(src, item)
        d = os.path.join(dst, item)
        if os.path.isdir(s):
            copytree(s, d, symlinks, ignore)
        else:
            if not os.path.exists(d) or os.stat(s).st_mtime - os.stat(d).st_mtime > 1:
                shutil.copy2(s, d)

def pullTestSuite(library, destDir, **kwargs):
    """downloads and unpacks a test suite"""
    mx.ensure_dir_exists(destDir)
    localPath = mx.library(library).get_path(True)
    tar(localPath, destDir, **kwargs)
    os.remove(localPath)
    sha1Path = localPath + '.sha1'
    if os.path.exists(sha1Path):
        os.remove(sha1Path)

def truffle_extract_VM_args(args, useDoubleDash=False):
    vmArgs, remainder = [], []
    if args is not None:
        for (i, arg) in enumerate(args):
            if any(arg.startswith(prefix) for prefix in ['-X', '-G:', '-D', '-verbose', '-ea', '-da', '-agentlib']) or arg in ['-esa']:
                vmArgs += [arg]
            elif useDoubleDash and arg == '--':
                remainder += args[i:]
                break
            else:
                remainder += [arg]
    return vmArgs, remainder


def extract_compiler_args(args, useDoubleDash=False):
    compilerArgs, remainder = [], []
    if args is not None:
        for (_, arg) in enumerate(args):
            if any(arg.startswith(prefix) for prefix in ['-']):
                compilerArgs += [arg]
            else:
                remainder += [arg]
    return compilerArgs, remainder

def extract_arg_values(vmArgs, argKey):
    values, remainder = [], []
    for arg in vmArgs:
        if arg.startswith(argKey + '='):
            values += arg[(len(argKey)+1):].split(':')
        else:
            remainder += [arg]
    return values, remainder

def runLLVM(args=None, out=None):
    """uses Sulong to execute a LLVM IR file"""
    vmArgs, sulongArgs = truffle_extract_VM_args(args)
    return mx.run_java(getCommonOptions(False) + substituteLibAliases(vmArgs) + getClasspathOptions() + ["com.oracle.truffle.llvm.launcher.LLVMLauncher"] + sulongArgs, out=out)

def getCommonOptions(withAssertion, lib_args=None):
    options = ['-Dgraal.TruffleCompilationExceptionsArePrinted=true',
        '-Dgraal.ExitVMOnException=true']

    if lib_args is not None:
        options.append('-Dpolyglot.llvm.libraries=' + ':'.join(lib_args))

    options += ['-Xss56m', '-Xms4g', '-Xmx4g']
    options.append(getLLVMRootOption())
    if withAssertion:
        options += ['-ea', '-esa']

    return options

def substituteLibAliases(vmArgs):
    librariesOption = '-Dpolyglot.llvm.libraries'
    lib_args, substitutedVmArgs = extract_arg_values(vmArgs, librariesOption)
    if len(lib_args) == 0:
        return vmArgs

    lib_aliases = {
        'l(.*)rust' : '<rustlib:*>'
    }

    lib_aliases = {re.compile(k+'$'):v for k, v in lib_aliases.items()}
    resolved_lib_args = []
    for lib_arg in lib_args:
        for lib_alias, lib_alias_value in lib_aliases.items():
            match = lib_alias.match(lib_arg)
            if match:
                lib_arg = lib_alias_value
                if match.lastindex is not None:
                    lib_arg = lib_arg.replace('*', match.group(1))
                lib_arg = mx_subst.path_substitutions.substitute(lib_arg)
        resolved_lib_args.append(lib_arg)
    substitutedVmArgs.append(librariesOption + '=' + ':'.join(resolved_lib_args))

    return substitutedVmArgs

def getLLVMRootOption():
    return "-Dsulongtest.projectRoot=" + _root

def pullsuite(suiteDir, urls):
    name = os.path.basename(urls[0])
    localPath = join(suiteDir, name)
    mx.download(localPath, urls)
    return localPath

def isSupportedLLVMVersion(llvmProgram, supportedVersions=None):
    """returns if the LLVM program bases on a supported LLVM version"""
    assert llvmProgram is not None
    llvmVersion = getLLVMVersion(llvmProgram)
    if supportedVersions is None:
        return llvmVersion in supportedLLVMVersions
    else:
        return llvmVersion in supportedVersions

def isSupportedGCCVersion(gccProgram, supportedVersions=None):
    """returns if the LLVM program bases on a supported LLVM version"""
    assert gccProgram is not None
    gccVersion = getGCCVersion(gccProgram)
    if supportedVersions is None:
        return gccVersion in supportedGCCVersions
    else:
        return gccVersion in supportedVersions

def getVersion(program):
    """executes --version on the supplied program and returns the version string"""
    assert program is not None
    try:
        versionString = subprocess.check_output([program, '--version'])
    except subprocess.CalledProcessError as e:
        # on my machine, e.g., opt returns a non-zero opcode even on success
        versionString = e.output
    return versionString

def getLLVMVersion(llvmProgram):
    """executes the program with --version and extracts the LLVM version string"""
    versionString = getVersion(llvmProgram)
    printLLVMVersion = re.search(r'(clang |LLVM )?(version )?((\d)\.\d)(\.\d)?', versionString, re.IGNORECASE)
    if printLLVMVersion is None:
        return None
    else:
        return printLLVMVersion.group(3)

# the makefiles do not check which version of clang they invoke
clang_versions_need_optnone = ['5', '6']
def getLLVMExplicitArgs(mainLLVMVersion):
    if mainLLVMVersion:
        for ver in clang_versions_need_optnone:
            if mainLLVMVersion.startswith(ver):
                return ["-Xclang", "-disable-O0-optnone"]
    return []

def getClangImplicitArgs():
    mainLLVMVersion = getLLVMVersion('clang')
    return " ".join(getLLVMExplicitArgs(mainLLVMVersion))

mx_subst.path_substitutions.register_no_arg('clangImplicitArgs', getClangImplicitArgs)

def getGCCVersion(gccProgram):
    """executes the program with --version and extracts the GCC version string"""
    versionString = getVersion(gccProgram)
    gccVersion = re.search(r'((\d\.\d).\d)', versionString, re.IGNORECASE)
    if gccVersion is None:
        exit("could not find the GCC version string in " + str(versionString))
    else:
        return gccVersion.group(2)

def findInstalledLLVMProgram(llvmProgram, supportedVersions=None):
    """tries to find a supported version of a program by checking for the argument string (e.g., clang) and appending version numbers (e.g., clang-3.4) as specified by the postfixes (or supportedLLVMVersions by default)"""
    if supportedVersions is None:
        appends = supportedLLVMVersions
    else:
        appends = supportedVersions
    return findInstalledProgram(llvmProgram, appends, isSupportedLLVMVersion)

def findInstalledGCCProgram(gccProgram):
    """tries to find a supported version of a GCC program by checking for the argument string (e.g., gfortran) and appending version numbers (e.g., gfortran-4.9)"""
    path = None
    if 'DRAGONEGG_GCC' in os.environ:
        path = [os.path.join(os.environ['DRAGONEGG_GCC'], 'bin')]
    return findInstalledProgram(gccProgram, supportedGCCVersions, isSupportedGCCVersion, searchPath=path)

def findInstalledProgram(program, supportedVersions, testSupportedVersion, searchPath=None):
    """tries to find a supported version of a program

    The function takes program argument, and checks if it has the supported version.
    If not, it prepends a supported version to the version string to check if it is an executable program with a supported version.
    The function checks both for programs by appending "-" and the unmodified version string, as well as by directly adding all the digits of the version string (stripping all other characters).

    For example, for a program gcc with supportedVersions 4.6 the function produces gcc-4.6 and gcc46.

    Arguments:
    program -- the program to find, e.g., clang or gcc
    supportedVersions -- the supported versions, e.g., 3.4 or 4.9
    testSupportedVersion(path, supportedVersions) -- the test function to be called to ensure that the found program is supported
    searchPath -- search path to find binaries (defaults to PATH environment variable)
    """
    assert program is not None
    programPath = which(program, searchPath=searchPath)
    if programPath is not None and testSupportedVersion(programPath, supportedVersions):
        return programPath
    else:
        for version in supportedVersions:
            alternativeProgram1 = program + '-' + version
            alternativeProgram2 = program + re.sub(r"\D", "", version)
            alternativePrograms = [alternativeProgram1, alternativeProgram2]
            for alternativeProgram in alternativePrograms:
                alternativeProgramPath = which(alternativeProgram, searchPath=searchPath)
                if alternativeProgramPath is not None:
                    assert testSupportedVersion(alternativeProgramPath, supportedVersions)
                    return alternativeProgramPath
    return None

def findLLVMProgram(llvmProgram, version=None):
    """tries to find a supported version of the given LLVM program; exits if none can be found"""
    installedProgram = findInstalledLLVMProgram(llvmProgram, version)

    if installedProgram is None:
        exit('found no supported version of ' + llvmProgram)
    else:
        return installedProgram

def findGCCProgram(gccProgram, optional=False):
    """tries to find a supported version of an installed GCC program"""
    installedProgram = findInstalledGCCProgram(gccProgram)
    if installedProgram is None and not optional:
        exit('found no supported version ' + str(supportedGCCVersions) + ' of ' + gccProgram)
    else:
        return installedProgram

def findRustLibrary(name, on_failure=exit):
    """looks up the path to the given Rust library for the active toolchain; may try to install the active toolchain if it is missing; exits by default if installation fails"""
    if not checkRustComponent('rustc'):
        on_failure('Rust is not available')
        return None

    sysroot = subprocess.check_output(['rustc', '--print', 'sysroot']).rstrip()
    lib_paths = glob.glob(os.path.join(sysroot, 'lib', mx.add_lib_suffix('lib' + name + '-*')))
    if len(lib_paths) == 0:
        on_failure('could not find Rust library ' + name)
        return None
    else:
        return lib_paths[0]

mx_subst.path_substitutions.register_with_arg('rustlib', findRustLibrary)

def getClasspathOptions():
    """gets the classpath of the Sulong distributions"""
    return mx.get_runtime_jvm_args(['SULONG', 'SULONG_LAUNCHER'])

def ensureLLVMBinariesExist():
    """downloads the LLVM binaries if they have not been downloaded yet"""
    for llvmBinary in basicLLVMDependencies:
        if findLLVMProgram(llvmBinary) is None:
            raise Exception(llvmBinary + ' not found')

_env_flags = []
if 'CPPFLAGS' in os.environ:
    _env_flags = os.environ['CPPFLAGS'].split(' ')


# used by mx_sulong_benchmarks:

def opt(args=None, version=None, out=None, err=None):
    """runs opt"""
    return mx.run([findLLVMProgram('opt', version)] + args, out=out, err=err)

# Project classes

class ArchiveProject(mx.ArchivableProject):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        mx.ArchivableProject.__init__(self, suite, name, deps, workingSets, theLicense)
        assert 'prefix' in args
        assert 'outputDir' in args

    def output_dir(self):
        return join(self.dir, self.outputDir)

    def archive_prefix(self):
        return self.prefix

    def getResults(self):
        return mx.ArchivableProject.walk(self.output_dir())

class SulongDocsProject(ArchiveProject):
    doc_files = (glob.glob(join(_suite.dir, 'LICENSE')) +
        glob.glob(join(_suite.dir, '*.md')))

    def getResults(self):
        return [join(_suite.dir, f) for f in self.doc_files]


mx_benchmark.add_bm_suite(mx_sulong_benchmarks.SulongBenchmarkSuite())

mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_suite,
    name='Sulong',
    short_name='slg',
    dir_name='llvm',
    license_files=[],
    third_party_license_files=[],
    truffle_jars=['sulong:SULONG'],
    support_distributions=[
        'sulong:SULONG_LIBS',
        'sulong:SULONG_GRAALVM_DOCS',
    ],
    launcher_configs=[
        mx_sdk.LanguageLauncherConfig(
            destination='bin/<exe:lli>',
            jar_distributions=['sulong:SULONG_LAUNCHER'],
            main_class='com.oracle.truffle.llvm.launcher.LLVMLauncher',
            build_args=['--language:llvm']
        )
    ],
))

COPYRIGHT_HEADER_BSD = """\
/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
// Checkstyle: stop
//@formatter:off
{0}
"""

def create_asm_parser(args=None, out=None):
    """create the inline assembly parser using antlr"""
    mx.suite("truffle").extensions.create_parser("com.oracle.truffle.llvm.asm.amd64", "com.oracle.truffle.llvm.asm.amd64", "InlineAssembly", COPYRIGHT_HEADER_BSD, args, out)

mx.update_commands(_suite, {
    'lli' : [runLLVM, ''],
    'test-llvm-image' : [_test_llvm_image, 'test a pre-built LLVM image'],
    'create-asm-parser' : [create_asm_parser, 'create the inline assembly parser using antlr'],
})
