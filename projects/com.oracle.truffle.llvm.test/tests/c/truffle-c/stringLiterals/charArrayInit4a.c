struct {
  char a;
  char b[];
} a3 = { 'o', "wx" };

int main() { return a3.b[0] + a3.b[1] + a3.b[2]; }
