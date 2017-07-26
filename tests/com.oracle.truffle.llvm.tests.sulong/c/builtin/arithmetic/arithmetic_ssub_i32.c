#ifndef __clang__
#include <stdbool.h>
bool __builtin_sub_overflow(signed int, signed int, signed int*);
#endif

int main(int argc, const char **argv) {
  signed int res;

  if (__builtin_sub_overflow((signed int)0x0, (signed int)0x0, &res)) {
    return -1;
  }

  if (__builtin_sub_overflow((signed int)0x0, (signed int)0x7FFFFFFF, &res)) {
    return -1;
  }

  if (!__builtin_sub_overflow((signed int)0x0, (signed int)0x80000000, &res)) {
    return -1;
  }

  if (__builtin_sub_overflow((signed int)0x0, (signed int)0x80000001, &res)) {
    return -1;
  }

  if (__builtin_sub_overflow((signed int)0x1, (signed int)0x7FFFFFFF, &res)) {
    return -1;
  }

  if (__builtin_sub_overflow((signed int)0x7FFFFFFF, (signed int)0x0, &res)) {
    return -1;
  }

  if (__builtin_sub_overflow((signed int)0x7FFFFFFF, (signed int)0x1, &res)) {
    return -1;
  }

  if (__builtin_sub_overflow((signed int)0x7FFFFFFF, (signed int)0x7FFFFFFF, &res)) {
    return -1;
  }

  if (!__builtin_sub_overflow((signed int)0x7FFFFFFF, (signed int)0x80000000, &res)) {
    return -1;
  }

  if (!__builtin_sub_overflow((signed int)0x7FFFFFFF, (signed int)0xFFFFFFFF, &res)) {
    return -1;
  }

  if (!__builtin_sub_overflow((signed int)0x80000000, (signed int)0x7FFFFFFF, &res)) {
    return -1;
  }

  if (__builtin_sub_overflow((signed int)0x80000000, (signed int)0x80000000, &res)) {
    return -1;
  }

  if (__builtin_sub_overflow((signed int)0x80000001, (signed int)0x1, &res)) {
    return -1;
  }

  if (!__builtin_sub_overflow((signed int)0x80000001, (signed int)0x2, &res)) {
    return -1;
  }

  return 0;
}
