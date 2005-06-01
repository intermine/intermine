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


/* Usage: pnmscalealpha <input ppm> <colour ppm> <alpha pgm> <reduction>
 */

int main(int argc, char **argv);

int main(int argc, char **argv)
{
	FILE *inputfile, *colourfile, *alphafile;
	int *red, *green, *blue, *alpha;
	int x, y, xsize, ysize, reduction, sreduction, state, maxval, sub_x, sub_y;
	char *inputfilename, *colourfilename, *alphafilename;
	int c, r, g, b;
	char buf[1000];

	inputfilename = argv[1];
	colourfilename = argv[2];
	alphafilename = argv[3];
	sscanf(argv[4], "%d", &reduction);
	sreduction = reduction * reduction;

//	fprintf(stderr, "Input: %s\nColour: %s\nAlpha: %s\nReduction: %d\n", inputfilename, colourfilename, alphafilename, reduction);
	
	inputfile = fopen(inputfilename, "r");

	c = fgetc(inputfile);
	if (c != 'P')
	{
		fprintf(stderr, "Not a pnm picture");
		exit(1);
	}
	c = fgetc(inputfile);
	if (c != '6')
	{
		fprintf(stderr, "Not a raw ppm picture");
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
//	fprintf(stderr, "xsize: %d\nysize: %d\nmaxval: %d\n", xsize, ysize, maxval);

	if ((xsize % reduction != 0) || (ysize % reduction != 0))
	{
		fprintf(stderr, "Picture size (%d, %d) not a multiple of reduction factor (%d).\n", xsize, ysize, reduction);
		exit(1);
	}
	xsize = xsize / reduction;
	ysize = ysize / reduction;

	colourfile = fopen(colourfilename, "w");
	alphafile = fopen(alphafilename, "w");
	
	sprintf(buf, "P6 %d %d 255\n", xsize, ysize);
	fputs(buf, colourfile);
	sprintf(buf, "P5 %d %d 255\n", xsize, ysize);
	fputs(buf, alphafile);
	
	red = (int *) malloc(xsize * 4);
	green = (int *) malloc(xsize * 4);
	blue = (int *) malloc(xsize * 4);
	alpha = (int *) malloc(xsize * 4);
	
	for (y=0; y<ysize; y++)
	{
		for (x=0; x<xsize; x++)
		{
			red[x] = 0;
			green[x] = 0;
			blue[x] = 0;
			alpha[x] = 0;
		}
		for (sub_y=0; sub_y<reduction; sub_y++)
		{
			for (x=0; x<xsize; x++)
			{
				for (sub_x=0; sub_x<reduction; sub_x++)
				{
					r = fgetc(inputfile);
					g = fgetc(inputfile);
					b = fgetc(inputfile);
					if ((r != maxval) || (g != maxval) || (b != maxval))
					{
						red[x] += r;
						green[x] += g;
						blue[x] += b;
						alpha[x]++;
					}
				}
			}
		}
		for (x=0; x<xsize; x++)
		{
			if (alpha[x]==0)
			{
				putc(255, colourfile);
				putc(255, colourfile);
				putc(255, colourfile);
				putc(0, alphafile);
			}
			else
			{
				putc((red[x] * 255) / maxval / alpha[x], colourfile);
				putc((green[x] * 255) / maxval / alpha[x], colourfile);
				putc((blue[x] * 255) / maxval / alpha[x], colourfile);
				putc((alpha[x] * 255) / sreduction, alphafile);
			}
		}
//		fprintf(stderr, "Row %d\n", y);
	}
	fclose(colourfile);
	fclose(alphafile);
	free(red);
	free(green);
	free(blue);
	free(alpha);
}
