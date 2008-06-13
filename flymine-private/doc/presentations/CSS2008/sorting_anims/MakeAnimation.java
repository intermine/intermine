import java.io.*;
import java.util.*;

public class MakeAnimation
{
    public static final int MAX_IMAGES = 100;

    public static void main(String args[]) throws IOException
    {
        String basePath = args[0];
        int xsize = 0;
        int ysize = 0;
        // This program takes commands. The first command must be an image size
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        StringTokenizer inLine = new StringTokenizer(in.readLine());
        if (inLine.nextToken().equals("size")) {
            xsize = Integer.parseInt(inLine.nextToken());
            ysize = Integer.parseInt(inLine.nextToken());
        } else {
            throw new IllegalArgumentException("First command must be image size");
        }

        int frameNo = 0;
        Pnm output = new Pnm(xsize, ysize);
        inLine = new StringTokenizer(in.readLine());
        int commandFrame = Integer.parseInt(inLine.nextToken());
        Pnm images[] = new Pnm[MAX_IMAGES];
        int xPositions[] = new int[MAX_IMAGES];
        int yPositions[] = new int[MAX_IMAGES];
        int xImages[] = new int[MAX_IMAGES];
        int yImages[] = new int[MAX_IMAGES];
        int xMove[] = new int[MAX_IMAGES];
        int yMove[] = new int[MAX_IMAGES];
        int startMove[] = new int[MAX_IMAGES];
        int framesMove[] = new int[MAX_IMAGES];
        boolean swing[] = new boolean[MAX_IMAGES];
        do {
            while (frameNo < commandFrame) {
                output.fillWhite();
                for (int i = 0; i < MAX_IMAGES; i++) {
                    if (images[i] != null) {
                        int x = xImages[i];
                        int y = yImages[i];
                        if (frameNo >= startMove[i] && frameNo < startMove[i] + framesMove[i]) {
                            double progress = (frameNo - startMove[i] + 1.0) / (framesMove[i] + 1.0);
                            progress = accel(progress);
                            if (swing[i]) {
                                // Rotate about mid-point. Here we actually use 2 times midpoint
                                int xMid = 2 * x + xMove[i];
                                int yMid = 2 * y + yMove[i];
                                double xRot = Math.cos(progress * Math.PI) * xMove[i] - Math.sin(progress * Math.PI) * yMove[i];
                                double yRot = Math.sin(progress * Math.PI) * xMove[i] + Math.cos(progress * Math.PI) * yMove[i];
                                x = (int) ((xMid - xRot) / 2);
                                y = (int) ((yMid - yRot) / 2);
                            } else {
                                x = x + (int) (progress * xMove[i]);
                                y = y + (int) (progress * yMove[i]);
                            }
                        }
                        output.copyIn(images[i], x, y);
                        if (frameNo == startMove[i] + framesMove[i] - 1) {
                            xImages[i] += xMove[i];
                            yImages[i] += yMove[i];
                        }
                    }
                }
                String outFileName = basePath + (frameNo < 1000 ? "0" : "") + (frameNo < 100 ? "0" : "") + (frameNo < 10 ? "0" : "") + frameNo + ".pnm";
                System.err.println("Writing file " + outFileName);
                output.writeImage(outFileName);
                frameNo++;
            }
            while (commandFrame <= frameNo) {
                String command = inLine.nextToken();
                if ("stop".equals(command)) {
                    System.err.println("Stop command received at frame " + commandFrame);
                    return;
                } else if ("load".equals(command)) {
                    int index = Integer.parseInt(inLine.nextToken());
                    images[index] = new Pnm(inLine.nextToken());
                    xPositions[index] = -10000;
                    yPositions[index] = -10000;
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
                    int index = Integer.parseInt(inLine.nextToken());
                    int position = Integer.parseInt(inLine.nextToken());
                    int frames = Integer.parseInt(inLine.nextToken());
                    xMove[index] = xPositions[position] - xImages[index];
                    yMove[index] = yPositions[position] - yImages[index];
                    startMove[index] = frameNo;
                    framesMove[index] = frames;
                    swing[index] = false;
                } else if ("swing".equals(command)) {
                    int index = Integer.parseInt(inLine.nextToken());
                    int position = Integer.parseInt(inLine.nextToken());
                    int frames = Integer.parseInt(inLine.nextToken());
                    xMove[index] = xPositions[position] - xImages[index];
                    yMove[index] = yPositions[position] - yImages[index];
                    startMove[index] = frameNo;
                    framesMove[index] = frames;
                    swing[index] = true;
                } else {
                    throw new IllegalArgumentException("Unknown command " + command);
                }
                inLine = new StringTokenizer(in.readLine());
                commandFrame = Integer.parseInt(inLine.nextToken());
            }
        } while (true);
    }

    public static double accel(double in) {
        return (1.0 - Math.cos(in * Math.PI)) / 2.0;
    }
}
