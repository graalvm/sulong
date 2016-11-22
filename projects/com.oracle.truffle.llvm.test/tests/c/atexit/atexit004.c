#include <stdio.h>
#include <stdlib.h>

static void exit1() {
  int returnVal = 123;
  exit(returnVal);
}

int main() {
  atexit(exit1);
  exit(0);
}
