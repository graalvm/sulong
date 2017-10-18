#include <stdio.h>
#include "./callee.h"

int main(int argc, const char* argv[]) {
	
	MyClass c;
	c.doPrint();
	printf("test\n");
}