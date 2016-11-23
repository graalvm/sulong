#include "../assert.h"
#include <stdlib.h>

int main() {
  // ==
  assert_false(NAN == INF);
  assert_false(INF == NAN);
  assert_false(NAN == NAN);
  // !=
  assert_true(NAN != INF);
  assert_true(INF != NAN);
  assert_true(NAN != NAN);
  // <
  assert_false(NAN < INF);
  assert_false(INF < NAN);
  assert_false(NAN < NAN);
  // <=
  assert_false(NAN <= INF);
  assert_false(INF <= NAN);
  assert_false(NAN <= NAN);
  // >
  assert_false(NAN > INF);
  assert_false(INF > NAN);
  assert_false(NAN > NAN);
  // >=
  assert_false(NAN >= INF);
  assert_false(INF >= NAN);
  assert_false(NAN >= NAN);
}
