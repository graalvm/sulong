#include <stdio.h>
#include <math.h>

typedef double (*test_type)(double);

double test1(double val) {
	return val * 111.5234;
}

double test2(double val2) {
	return val2 * 94.5023;
}

double test3(double val) {
	return val * 111.5234;
}

double test4(double val2) {
	return val2 * 94.5023;
}

double test5(double val) {
	return val * 111.5234;
}

double test6(double val2) {
	return val2 * 94.5023;
}

double test7(double val) {
	return val * 111.5234;
}

double test8(double val2) {
	return val2 * 94.5023;
}

double test9(double val2) {
	return val2 * 94.5023;
}

#define SIZE 10

test_type getFunction(int i) {
	int val = i % SIZE;
	switch (val) {
		case 0: return &log2;
		case 1: return &test1;
		case 2: return &test2;
		case 3: return &test3;
		case 4: return &test4;
		case 5: return &test5;
		case 6: return &test6;
		case 7: return &test7;
		case 8: return &test8;
		case 9: return &test9;
	}
	return 0;
}

int callFunction() {
	double val;
	int i;
	test_type func;
	double currentVal = 2342;
	for (i = 0; i < 1000; i++) {
		currentVal = getFunction(i)(currentVal);
	}
	return currentVal;	
}

int main() {
	int i;
	int sum = 0;
	for (i = 0; i < 10000; i++) {
		sum += callFunction();
	}
	return sum;
}
