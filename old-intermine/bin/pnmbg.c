#include <stdio.h>

#define S_WHITESPACE_BEFORE_WIDTH 1
#define S_COMMENT_BEFORE_WIDTH 2
#define S_WIDTH 3
#define S_WHITESPACE_BEFORE_HEIGHT 4
#define S_COMMENT_BEFORE_HEIGHT 5
#define S_HEIGHT 6
#define S_WHITESPACE_BEFORE_MAXVAL 7
#define S_COMMENT_BEFORE_MAXVAL 8
#define S_MAXVAL 9
#define S_FINISHED 10

#define BG_R 244
#define BG_G 238
#define BG_B 255

/* Usage: pnmbg <colour pnm> <alpha pgm>
 */

int main(int argc, char **argv);
void parseppm(FILE *inputfile, int *xsizep, int *ysizep, int *maxvalp, int *type);

int main(int argc, char **argv)
{
	FILE *colourfile, *alphafile;
	int x, y, xsize, ysize, xsizea, ysizea, maxval, type, typea;
	char *colourfilename, *alphafilename;
	int r, g, b, a, ia;
	char buf[1000];

	colourfilename = argv[1];
	alphafilename = argv[2];
	
//	fprintf(stderr, "Input: %s\nColour: %s\nAlpha: %s\nReduction: %d\n", inputfilename, colourfilename, alphafilename, reduction);
	
	colourfile = fopen(colourfilename, "r");
	parseppm(colourfile, &xsize, &ysize, &maxval, &type);

	if (maxval!=255)
	{
		fprintf(stderr, "Colour maxval is not 255\n");
		exit(1);
	}
	
	alphafile = fopen(alphafilename, "r");
	parseppm(alphafile, &xsizea, &ysizea, &maxval, &typea);
	
	if (maxval!=255)
	{
		fprintf(stderr, "Alpha maxval is not 255\n");
		exit(1);
	}

	if (typea!=5)
	{
		fprintf(stderr, "Alpha is not a PGM\n");
		exit(1);
	}

	if ((xsize!=xsizea) || (ysize!=ysizea))
	{
		fprintf(stderr, "Colour picture size (%d, %d) not equal to alpha picture size (%d, %d).\n", xsize, ysize, xsizea, ysizea);
		exit(1);
	}
	
//	fprintf(stderr, "xsize: %d\nysize: %d\nmaxval: %d\n", xsize, ysize, maxval);

	printf("P6 %d %d 255\n", xsize, ysize);
	
	for (x=0; x<xsize; x++)
	{
		for (y=0; y<ysize; y++)
		{
			r = fgetc(colourfile);
			if (type==6)
			{
				g = fgetc(colourfile);
				b = fgetc(colourfile);
			}
			else
			{
				g = r;
				b = r;
			}
			a = fgetc(alphafile);
			ia = 255-a;
			r = ((r * a) + (BG_R * ia))/255;
			g = ((g * a) + (BG_G * ia))/255;
			b = ((b * a) + (BG_B * ia))/255;
			putchar(r);
			putchar(g);
			putchar(b);
		}
	}
}

void parseppm(FILE *inputfile, int *xsizep, int *ysizep, int *maxvalp, int *type)
{
	int xsize, ysize, maxval, state, c;
	
	c = fgetc(inputfile);
	if (c != 'P')
	{
		fprintf(stderr, "Not a pnm picture.\n");
		exit(1);
	}
	c = fgetc(inputfile);
	*type = c - '0';
	if ((c != '6') && (c != '5'))
	{
		fprintf(stderr, "Not a raw ppm picture.\n");
		exit(1);
	}
	state = S_WHITESPACE_BEFORE_WIDTH;
	while (c != EOF && state != S_FINISHED)
	{
		c = fgetc(inputfile);
		switch(state)
		{
			case S_WHITESPACE_BEFORE_WIDTH:
				if (c >= '1' && c <= '9')
				{
					state = S_WIDTH;
					xsize = c - '0';
				}
				else if (c == '#') state = S_COMMENT_BEFORE_WIDTH;
				break;
			case S_COMMENT_BEFORE_WIDTH:
				if (c == '\n') state = S_WHITESPACE_BEFORE_WIDTH;
				break;
			case S_WIDTH:
				if (c >= '0' && c <= '9')
				{
					xsize = (xsize * 10) + c - '0';
				}
				else if (c == '#') state = S_COMMENT_BEFORE_HEIGHT;
				else state = S_WHITESPACE_BEFORE_HEIGHT;
				break;
			case S_WHITESPACE_BEFORE_HEIGHT:
				if (c >= '1' && c <= '9')
				{
					state = S_HEIGHT;
					ysize = c - '0';
				}
				else if (c == '#') state = S_COMMENT_BEFORE_HEIGHT;
				break;
			case S_COMMENT_BEFORE_HEIGHT:
				if (c == '\n') state = S_WHITESPACE_BEFORE_HEIGHT;
				break;
			case S_HEIGHT:
				if (c >= '0' && c <= '9')
				{
					ysize = (ysize * 10) + c - '0';
				}
				else if (c == '#') state = S_COMMENT_BEFORE_MAXVAL;
				else state = S_WHITESPACE_BEFORE_MAXVAL;
				break;
			case S_WHITESPACE_BEFORE_MAXVAL:
				if (c >= '1' && c <= '9')
				{
					state = S_MAXVAL;
					maxval = c - '0';
				}
				else if (c == '#') state = S_COMMENT_BEFORE_MAXVAL;
				break;
			case S_COMMENT_BEFORE_MAXVAL:
				if (c == '\n') state = S_WHITESPACE_BEFORE_MAXVAL;
				break;
			case S_MAXVAL:
				if (c >= '0' && c <= '9')
				{
					maxval = (maxval * 10) + c - '0';
				}
				else state = S_FINISHED;
				break;
		}
	}
	*xsizep = xsize;
	*ysizep = ysize;
	*maxvalp = maxval;
}
