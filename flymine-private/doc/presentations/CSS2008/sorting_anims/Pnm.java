import java.io.*;

public class Pnm
{
    public int xsize, ysize;
    public byte buf[];

    public Pnm(int xsize, int ysize) {
        this.xsize = xsize;
        this.ysize = ysize;
        buf = new byte[xsize * ysize * 3];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) 255;
        }
    }

    private static final int S_WHITESPACE_BEFORE_WIDTH = 1;
    private static final int S_COMMENT_BEFORE_WIDTH = 2;
    private static final int S_WIDTH = 3;
    private static final int S_WHITESPACE_BEFORE_HEIGHT = 4;
    private static final int S_COMMENT_BEFORE_HEIGHT = 5;
    private static final int S_HEIGHT = 6;
    private static final int S_WHITESPACE_BEFORE_MAXVAL = 7;
    private static final int S_COMMENT_BEFORE_MAXVAL = 8;
    private static final int S_MAXVAL = 9;
    private static final int S_FINISHED = 10;

    public Pnm(InputStream in) throws IOException {
        int maxval = 0;
        int c = in.read();
        if (c != 'P') {
            throw new IllegalArgumentException("Not a pnm picture");
        }
        c = in.read();
        if (c != '6') {
            throw new IllegalArgumentException("Not a raw colour (type 6) pnm picture");
        }
        int state = S_WHITESPACE_BEFORE_WIDTH;
        while (c != -1 && state != S_FINISHED) {
            c = in.read();
            switch(state) {
                case S_WHITESPACE_BEFORE_WIDTH:
                    if (c >= '1' && c <= '9') {
                        state = S_WIDTH;
                        xsize = c - '0';
                    } else if (c == '#') {
                        state = S_COMMENT_BEFORE_WIDTH;
                    }
                    break;
                case S_COMMENT_BEFORE_WIDTH:
                    if (c == '\n') {
                        state = S_WHITESPACE_BEFORE_WIDTH;
                    }
                    break;
                case S_WIDTH:
                    if (c >= '0' && c <= '9') {
                        xsize = (xsize * 10) + c - '0';
                    } else if (c == '#') {
                        state = S_COMMENT_BEFORE_HEIGHT;
                    } else if (c == ' ' || c == '\t' || c == '\n') {
                        state = S_WHITESPACE_BEFORE_HEIGHT;
                    } else {
                        throw new IllegalArgumentException("Illegal character " + ((char) c));
                    }
                    break;
                case S_WHITESPACE_BEFORE_HEIGHT:
                    if (c >= '1' && c <= '9') {
                        state = S_HEIGHT;
                        ysize = c - '0';
                    } else if (c == '#') {
                        state = S_COMMENT_BEFORE_HEIGHT;
                    }
                    break;
                case S_COMMENT_BEFORE_HEIGHT:
                    if (c == '\n') {
                        state = S_WHITESPACE_BEFORE_HEIGHT;
                    }
                    break;
                case S_HEIGHT:
                    if (c >= '0' && c <= '9') {
                        ysize = (ysize * 10) + c - '0';
                    } else if (c == '#') {
                        state = S_COMMENT_BEFORE_MAXVAL;
                    } else if (c == ' ' || c == '\t' || c == '\n') {
                        state = S_WHITESPACE_BEFORE_MAXVAL;
                    } else {
                        throw new IllegalArgumentException("Illegal character " + ((char) c));
                    }
                    break;
                case S_WHITESPACE_BEFORE_MAXVAL:
                    if (c >= '1' && c <= '9') {
                        state = S_MAXVAL;
                        maxval = c - '0';
                    } else if (c == '#') {
                        state = S_COMMENT_BEFORE_MAXVAL;
                    }
                    break;
                case S_COMMENT_BEFORE_MAXVAL:
                    if (c == '\n') {
                        state = S_WHITESPACE_BEFORE_MAXVAL;
                    }
                    break;
                case S_MAXVAL:
                    if (c >= '0' && c <= '9') {
                        maxval = (maxval * 10) + c - '0';
                    } else if (c == ' ' || c == '\t' || c == '\n') {
                        state = S_FINISHED;
                    } else {
                        throw new IllegalArgumentException("Illegal character " + ((char) c));
                    }
                    break;
            }
        }
        if (maxval != 255) {
            throw new IllegalArgumentException("Maxval is not 255");
        }
        buf = new byte[xsize * ysize * 3];
        int amountRead = in.read(buf);
        if (amountRead != buf.length) {
            throw new IOException("Too little data read - wanted " + buf.length + " but got " + amountRead);
        }
    }

    public Pnm(String fileName) throws IOException {
        this(new FileInputStream(fileName));
    }

    public void fillWhite() {
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) 255;
        }
    }

    public void copyIn(Pnm source, int xo, int yo) {
        int ymax = Math.min(source.ysize, ysize - yo);
        for (int y = Math.max(0, 0 - yo); y < ymax; y++) {
            int yd = yo + y;
            int xmax = Math.min(source.xsize, xsize - xo);
            for (int x = Math.max(0, 0 - xo); x < xmax; x++) {
                int xd = xo + x;
                int id = (xd + (yd * xsize)) * 3;
                int i = (x + (y * source.xsize)) * 3;
                buf[id] = (byte) Math.min((buf[id] + 512) & 0xFF, (source.buf[i] + 512) & 0xFF);
                buf[id + 1] = (byte) Math.min((buf[id + 1] + 512) & 0xFF, (source.buf[i + 1] + 512) & 0xFF);
                buf[id + 2] = (byte) Math.min((buf[id + 2] + 512) & 0xFF, (source.buf[i + 2] + 512) & 0xFF);
            }
        }
    }

    public void copyIn(Pnm source, int xo, int yo, double fade) {
        int ymax = Math.min(source.ysize, ysize - yo);
        for (int y = Math.max(0, 0 - yo); y < ymax; y++) {
            int yd = yo + y;
            int xmax = Math.min(source.xsize, xsize - xo);
            for (int x = Math.max(0, 0 - xo); x < xmax; x++) {
                int xd = xo + x;
                int id = (xd + (yd * xsize)) * 3;
                int i = (x + (y * source.xsize)) * 3;
                buf[id] = (byte) Math.min((buf[id] + 512) & 0xFF, ((int) ((((source.buf[i] + 512) & 0xFF) - 255) * fade)) + 255);
                buf[id + 1] = (byte) Math.min((buf[id + 1] + 512) & 0xFF, ((int) ((((source.buf[i + 1] + 512) & 0xFF) - 255) * fade)) + 255);
                buf[id + 2] = (byte) Math.min((buf[id + 2] + 512) & 0xFF, ((int) ((((source.buf[i + 2] + 512) & 0xFF) - 255) * fade)) + 255);
            }
        }
    }

    public void writeImage(PrintStream out) throws IOException {
        out.println("P6 " + xsize + " " + ysize + " 255");
        out.write(buf);
        if (out.checkError()) {
            throw new IOException("Error writing picture");
        }
    }

    public void writeImage(String fileName) throws IOException {
        writeImage(new PrintStream(fileName));
    }
}
