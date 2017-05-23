[Flang](https://github.com/flang-compiler/flang) is a Fortran compiler targeting
LLVM, which would allow us to compile Fortran code directly into LLVM-IR of
version 3.9 and higher. This writeup will document the current state of flang,
dating 23 Mai 2017, and how to get it running with sulong.

# build flang

Short notice: I'm working on an Arch-Linux derivate, which has clang 4.0.0 installed.

## Install dependencies

```bash
sudo pacman -S llvm clang openmp
```

## Create basic directory

```bash
mkdir flang-dev && cd flang-dev
```

## Compile modificated clang

```bash
git clone https://github.com/flang-compiler/clang.git -b flang_release_40
cd clang
mkdir build && cd build
cmake ..
make -j 4
sudo make install
cd ../..
```

You have to note the ```sudo make install```. Unfortunately this modified clang
also installs a dummy ```flang``` binary, which is required for later compilation.
The binaries are installed by default into ```/usr/local/bin/```. Flang itself is
only a link to the modified clang binary.

## Compile flang

```bash
git clone https://github.com/flang-compiler/flang.git
cd flang
mkdir build && cd build
# you need to adjust the compiler paths accordingly
cmake -DCMAKE_CXX_COMPILER=/home/user/dir/flang/clang/build/bin/clang++ \
      -DCMAKE_C_COMPILER=/home/user/dir/flang/clang/build/bin/clang \
      -DCMAKE_Fortran_COMPILER=flang \
      -DTARGET_ARCHITECTURE=x86_64 \
      -DTARGET_OS=Linux ..
make -j 4
sudo make install
```

On Arch Linux, the target architecture wasn't detected correctly by CMake, which
means we were required to overwrite it. Furthermore, I was required to patch the
flang source a bit, to fix compilation errors:
[https://github.com/pointhi/flang/commit/ef09e8](https://github.com/pointhi/flang/commit/ef09e8912a7194378b1a1239eb51d48da226c26d)

# Compile and run a fortran program

For now, we only want to show how to run a simple fortran binary on sulong

```fortran
program hello
  print *, 'hello world'
  end
```

## Compile to Bitcode

The Emited LLVM-IR has some little difference, which requires us to patch it either
in the .ll file itself, or extend sulong to be able to run such a binary. For this
tutorial, I will patch the ```.ll``` file. Updating sulong to run such files directly
is however only a two line patch. The cause is that the main function is named
different. ```@main``` of the original Clang vs ```@MAIN_``` which is currently
used by flang.

```bash
# compile to LLVM-IR
flang -S -emit-llvm -o fortran/hello_world.ll fortran/hello_world.f90
# call target is named different in flang
sed -i "s/@MAIN_/@main/g" fortran/hello_world.ll
# convert to bitcode
llvm-as fortran/hello_world.ll
```

## Run .bc file with Sulong

To run the fortran code, we are required to load a runtime library
located at ```/usr/local/lib/libflang.so```.

```bash
mx su-run -Dsulong.DynamicNativeLibraryPath=/usr/local/lib/libflang.so fortran/hello_world.bc
```

# Conclusion

I was able to modificate the ```gcc38``` in such a way to run the fortran tests
using flang. The first try of running it resulted in about 700 Compilation Errors,
which are about 1/4 of the fortran testsuite. Tracking those errors down, they
seem to be mostly caused by the usage of fortran language features which are not
implemented yet in flang, or will probably never implemented:

* ```gfortran.dg/pr47757-1.f90```only works using ```-std=f2008``` with ```gfortran```,
  otherwise same error messages as by flang
* ```gfortran.dg/alloc_comp_basics_1.f90```
  only works using ```-std=gnu``` with ```gfortran```
* ```gfortran.dg/assumed_charlen_function_5.f90``` seems to use language features
  not yet implemented in flang
* ...

At the moment, there doesn't seem to be much point in using flang as draggonegg
replacement for sulong. Nevertheless, it should be noted that this project is backed
by NVIDIA as well as the US Department of Energy's National Nuclear Security Administration.
Which means it could grow to a full draggonegg replacement in the future:
[FLANG: NVIDIA Brings Fortran To LLVM](https://phoronix.com/scan.php?page=news_item&px=LLVM-NVIDIA-Fortran-Flang).

A initial patchset to get flang running with sulong can be found on my
github repository: [https://github.com/pointhi/sulong/commit/e9169d](https://github.com/pointhi/sulong/commit/e9169d9339d6e1df76f3779332b663cbc7a56924)
