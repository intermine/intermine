import java.io.*;
import java.util.*;

public class MakeAnimation
{
    public static final int MAX_IMAGES = 100;
    public static final int MT_MOVE = 0;
    public static final int MT_SWING = 1;
    public static final int MT_FLASH = 2;
    public static final int MT_FADEIN = 3;
    public static final int MT_FADEOUT = 4;

    public static void main(String args[]) throws IOException
    {
        int linesRead = 0;
        try {
            String basePath = args[0];
            int xsize = 0;
            int ysize = 0;
            // This program takes commands. The first command must be an image size
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

            linesRead++;
            StringTokenizer inLine = new StringTokenizer(in.readLine());
            if (inLine.nextToken().equals("size")) {
                xsize = Integer.parseInt(inLine.nextToken());
                ysize = Integer.parseInt(inLine.nextToken());
            } else {
                throw new IllegalArgumentException("First command must be image size");
            }

            int frameNo = 0;
            Pnm output = new Pnm(xsize, ysize);
            int commandFrame = 0;
            Pnm images[] = new Pnm[MAX_IMAGES];
            int xPositions[] = new int[MAX_IMAGES];
            int yPositions[] = new int[MAX_IMAGES];
            int xImages[] = new int[MAX_IMAGES];
            int yImages[] = new int[MAX_IMAGES];
            int xMove[] = new int[MAX_IMAGES];
            int yMove[] = new int[MAX_IMAGES];
            int startMove[] = new int[MAX_IMAGES];
            int framesMove[] = new int[MAX_IMAGES];
            int moveType[] = new int[MAX_IMAGES];
            do {
                while (frameNo < commandFrame) {
                    output.fillWhite();
                    for (int i = 0; i < MAX_IMAGES; i++) {
                        if (images[i] != null) {
                            int x = xImages[i];
                            int y = yImages[i];
                            if (frameNo >= startMove[i] && frameNo < startMove[i] + framesMove[i]) {
                                double progress = (frameNo - startMove[i] + 1.0) / (framesMove[i] + 1.0);
                                if (moveType[i] == MT_SWING) {
                                    // Rotate about mid-point. Here we actually use 2 times midpoint
                                    progress = (1.0 - Math.cos(progress * Math.PI)) / 2.0;
                                    int xMid = 2 * x + xMove[i];
                                    int yMid = 2 * y + yMove[i];
                                    double xRot = Math.cos(progress * Math.PI) * xMove[i] - Math.sin(progress * Math.PI) * yMove[i];
                                    double yRot = Math.sin(progress * Math.PI) * xMove[i] + Math.cos(progress * Math.PI) * yMove[i];
                                    x = (int) ((xMid - xRot) / 2);
                                    y = (int) ((yMid - yRot) / 2);
                                    output.copyIn(images[i], x, y);
                                } else if (moveType[i] == MT_MOVE) {
                                    progress = (1.0 - Math.cos(progress * Math.PI)) / 2.0;
                                    x = x + (int) (progress * xMove[i]);
                                    y = y + (int) (progress * yMove[i]);
                                    output.copyIn(images[i], x, y);
                                } else if (moveType[i] == MT_FLASH) {
                                    progress = Math.sin(progress * Math.PI);
                                    if (x == -10000) {
                                        // Fade in and out.
                                        output.copyIn(images[i], xMove[i], yMove[i], progress);
                                    } else {
                                        x = x + (int) (progress * (xMove[i] - xImages[i]));
                                        y = y + (int) (progress * (yMove[i] - yImages[i]));
                                        output.copyIn(images[i], x, y);
                                    }
                                } else if (moveType[i] == MT_FADEIN) {
                                    progress = Math.sin(progress * Math.PI * 0.5);
                                    output.copyIn(images[i], xMove[i], yMove[i], progress);
                                } else if (moveType[i] == MT_FADEOUT) {
                                    progress = Math.cos(progress * Math.PI * 0.5);
                                    output.copyIn(images[i], xImages[i], yImages[i], progress);
                                }
                            } else {
                                output.copyIn(images[i], x, y);
                            }
                            if (frameNo == startMove[i] + framesMove[i] - 1) {
                                switch (moveType[i]) {
                                    case MT_SWING:
                                    case MT_MOVE:
                                        xImages[i] += xMove[i];
                                        yImages[i] += yMove[i];
                                        break;
                                    case MT_FADEIN:
                                        xImages[i] = xMove[i];
                                        yImages[i] = yMove[i];
                                        break;
                                    case MT_FADEOUT:
                                        xImages[i] = -10000;
                                        yImages[i] = -10000;
                                        break;
                                }
                            }
                        }
                    }
                    String outFileName = basePath + (frameNo < 1000 ? "0" : "") + (frameNo < 100 ? "0" : "") + (frameNo < 10 ? "0" : "") + frameNo + ".pnm";
                    System.err.println("Writing file " + outFileName);
                    output.writeImage(outFileName);
                    frameNo++;
                }
                while (commandFrame <= frameNo) {
                    linesRead++;
                    inLine = new StringTokenizer(in.readLine());
                    String command = inLine.nextToken();
                    if ("stop".equals(command)) {
                        System.err.println("Stop command received at frame " + commandFrame);
                        return;
                    } else if ("advance".equals(command)) {
                        commandFrame += Integer.parseInt(inLine.nextToken());
                    } else if ("load".equals(command)) {
                        int index = Integer.parseInt(inLine.nextToken());
                        images[index] = new Pnm(inLine.nextToken());
                        xImages[index] = -10000;
                        yImages[index] = -10000;
                    } else if ("position".equals(command)) {
                        int index = Integer.parseInt(inLine.nextToken());
                        xPositions[index] = Integer.parseInt(inLine.nextToken());
                        yPositions[index] = Integer.parseInt(inLine.nextToken());
                    } else if ("place".equals(command)) {
                        int index = Integer.parseInt(inLine.nextToken());
                        int position = Integer.parseInt(inLine.nextToken());
                        if (position == -1) {
                            xImages[index] = -10000;
                            yImages[index] = -10000;
                        } else {
                            xImages[index] = xPositions[position];
                            yImages[index] = yPositions[position];
                        }
                    } else if ("move".equals(command)) {
                        // Moves an image along a straight line to a new position
                        int index = Integer.parseInt(inLine.nextToken());
                        int position = Integer.parseInt(inLine.nextToken());
                        int frames = Integer.parseInt(inLine.nextToken());
                        xMove[index] = xPositions[position] - xImages[index];
                        yMove[index] = yPositions[position] - yImages[index];
                        startMove[index] = frameNo;
                        framesMove[index] = frames;
                        moveType[index] = MT_MOVE;
                    } else if ("swing".equals(command)) {
                        // Swings an image through a half-circle to a new position
                        int index = Integer.parseInt(inLine.nextToken());
                        int position = Integer.parseInt(inLine.nextToken());
                        int frames = Integer.parseInt(inLine.nextToken());
                        xMove[index] = xPositions[position] - xImages[index];
                        yMove[index] = yPositions[position] - yImages[index];
                        startMove[index] = frameNo;
                        framesMove[index] = frames;
                        moveType[index] = MT_SWING;
                    } else if ("flash".equals(command)) {
                        // Flashes up an image for a period of time. If the image is already shown,
                        // then this moves it to the new position and back again. Otherwise, it
                        // fades it in and out again.
                        int index = Integer.parseInt(inLine.nextToken());
                        int position = Integer.parseInt(inLine.nextToken());
                        int frames = Integer.parseInt(inLine.nextToken());
                        xMove[index] = xPositions[position];
                        yMove[index] = yPositions[position];
                        startMove[index] = frameNo;
                        framesMove[index] = frames;
                        moveType[index] = MT_FLASH;
                    } else if ("fadein".equals(command)) {
                        int index = Integer.parseInt(inLine.nextToken());
                        int position = Integer.parseInt(inLine.nextToken());
                        int frames = Integer.parseInt(inLine.nextToken());
                        xMove[index] = xPositions[position];
                        yMove[index] = yPositions[position];
                        startMove[index] = frameNo;
                        framesMove[index] = frames;
                        moveType[index] = MT_FADEIN;
                    } else if ("fadeout".equals(command)) {
                        int index = Integer.parseInt(inLine.nextToken());
                        int frames = Integer.parseInt(inLine.nextToken());
                        startMove[index] = frameNo;
                        framesMove[index] = frames;
                        moveType[index] = MT_FADEOUT;
                    } else if ("swap".equals(command)) {
                        // Swaps two images, using swings
                        int index1 = Integer.parseInt(inLine.nextToken());
                        int index2 = Integer.parseInt(inLine.nextToken());
                        int frames = Integer.parseInt(inLine.nextToken());
                        xMove[index1] = xImages[index2] - xImages[index1];
                        yMove[index1] = yImages[index2] - yImages[index1];
                        xMove[index2] = xImages[index1] - xImages[index2];
                        yMove[index2] = yImages[index1] - yImages[index2];
                        startMove[index1] = frameNo;
                        startMove[index2] = frameNo;
                        framesMove[index1] = frames;
                        framesMove[index2] = frames;
                        moveType[index1] = MT_SWING;
                        moveType[index2] = MT_SWING;
                    } else if ("#".equals(command)) {
                        // Comment - do nothing
                    } else {
                        throw new IllegalArgumentException("Unknown command: " + command);
                    }
                    if ((!("#".equals(command))) && inLine.hasMoreTokens()) {
                        throw new IllegalArgumentException("Too many arguments: " + inLine.nextToken());
                    }
                }
            } while (true);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("Error encountered on line " + linesRead + " of input");
            System.exit(1);
        }
    }
}
