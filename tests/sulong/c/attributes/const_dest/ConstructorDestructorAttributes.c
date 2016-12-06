#include <stdio.h>

// This test is an extended version of 
// SingleSource/Regression/C/ConstructorDestructorAttributes.c
// in the LLVMv3.2 Testsuite.

void ctor6() __attribute__((constructor (101)));

void ctor6() {
   printf("Create6!\n");
}

void ctor() __attribute__((constructor (101)));

void ctor() {
   printf("Create!\n");
}

void ctor2() __attribute__((constructor (102)));

void ctor2() {
   printf("Create2!\n");
}

void ctor3() __attribute__((constructor (103)));

void ctor3() {
   printf("Create3!\n");
}

void ctor4() __attribute__((constructor (104)));

void ctor4() {
   printf("Create4!\n");
}

void ctor5() __attribute__((constructor (104)));

void ctor5() {
   printf("Create5!\n");
}

void dtor6() __attribute__((destructor (102)));

void dtor6() {
   printf("Destroy6!\n");
}

void dtor() __attribute__((destructor (102)));

void dtor() {
   printf("Destroy!\n");
}

void dtor2() __attribute__((destructor (103)));

void dtor2() {
   printf("Destroy2!\n");
}

void dtor3() __attribute__((destructor (104)));

void dtor3() {
   printf("Destroy3!\n");
}

void dtor4() __attribute__((destructor (105)));

void dtor4() {
   printf("Destroy4!\n");
}

void dtor5() __attribute__((destructor (105)));

void dtor5() {
   printf("Destroy5!\n");
}

int main() { return 0; }
