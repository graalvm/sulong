int main() {
  int a[50] = { 50, 49, 48, 47, 46, 45, 44, 43, 42, 41, 40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26,
                25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9,  8,  7,  6,  5,  4,  3,  2,  1 };
  int n = 50;
  int i, j;
  int iMin;
  int temp;

  for (j = 0; j < n - 1; j++) {
    iMin = j;
    for (i = j + 1; i < n; i++) {
      if (a[i] < a[iMin]) {
        iMin = i;
      }
    }
    if (iMin != j) {
      temp = a[j];
      a[j] = a[iMin];
      a[iMin] = temp;
    }
  }
  return a[49];
}
