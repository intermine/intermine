import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InternalIndexPage extends IndexPage
{
    private static int splitCount = 0;
    private int maxPages, pageCount;
    private IndexPage[] pages;
    private PenaltyCalculator penaltyCalc;
    private SplitCalculator splitCalc;

    public InternalIndexPage(long colour, int maxPages, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc) {
        super(colour);
        this.maxPages = maxPages;
        this.penaltyCalc = penaltyCalc;
        this.splitCalc = splitCalc;
        pages = new IndexPage[maxPages + 1];
        pageCount = 0;
    }

    public InternalIndexPage(long colour, int maxPages, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc, SplitPage splitPage) {
        this(colour, maxPages, penaltyCalc, splitCalc);
        addPage(splitPage.getLeft());
        addPage(splitPage.getRight());
    }

    public void addEntry(IndexEntry entry) throws PageNeedsSplitException {
        double bestPenalty = Double.MAX_VALUE;
        int bestPage = -1;
        for (int i = 0; i < pageCount; i++) {
            double penalty = penaltyCalc.calc(pages[i], entry);
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                bestPage = i;
            }
        }
        expandToCover(entry);
        try {
            pages[bestPage].addEntry(entry);
        } catch (PageNeedsSplitException e) {
            IndexPage page = pages[bestPage];
            SplitPage splitPage = splitCalc.calc(page);
            pages[bestPage] = splitPage.getLeft();
            pages[pageCount++] = splitPage.getRight();
/*            splitCount++;
            if (splitCount % 100 == 0) {
                outputImageForSplit(page, splitCount, MidAverageSplitCalculator.INSTANCE);
                outputImageForSplit(page, splitCount, StartEndMidSplitCalculator.INSTANCE);
                outputImageForSplit(page, splitCount, SizeStartEndMidSplitCalculator.INSTANCE);
                outputImageForSplit(page, splitCount, GeometricAreaStartEndSplitCalculator.INSTANCE);
                outputImageForSplit(page, splitCount, GeometricSizeSplitCalculator.INSTANCE);
            }*/
            if (pageCount > maxPages) {
                throw new PageNeedsSplitException();
            }
        }
    }

    public static void outputImageForSplit(IndexPage page, int imageNumber, SplitCalculator splitCalc) {
        SplitPage splitPage = splitCalc.calc(page);
        String splitterName = splitCalc.getClass().getName();
        if (splitterName.indexOf(".") != -1) {
            splitterName = splitterName.substring(splitterName.lastIndexOf(".") + 1);
        }
        try {
            InternalIndexPage image = new InternalIndexPage(-1, 3, null, null);
            image.addPage(page);
            image.addPage(splitPage.getLeft());
            image.addPage(splitPage.getRight());
//            image.makeImage(1550, image.getMin(), image.getMax(), 3).writeImage("splits/split_" + TestBed.intToString(imageNumber, 5) + "_" + Integer.toHexString(page.hashCode()) + "_to_" + Integer.toHexString(retval.getLeft().hashCode()) + "_" + Integer.toHexString(retval.getRight().hashCode()) + ".pnm", 1550);
            image.makeImage(1550, image.getMin(), image.getMax(), 3).writeImage("splits/split_" + TestBed.intToString(imageNumber, 5) + "_" + splitterName + "_" + TestBed.intToString(splitPage.getLeft().entryCount(), 3) + "_" + TestBed.intToString(splitPage.getRight().entryCount(), 3) + ".pnm", 1550);
        } catch (IOException e2) {
            e2.printStackTrace(System.err);
        }
    }

    protected void addPage(IndexPage page) {
        pages[pageCount++] = page;
        expandToCover(page);
    }

    public int getMaxPages() {
        return maxPages;
    }

    public PenaltyCalculator getPenaltyCalc() {
        return penaltyCalc;
    }

    public SplitCalculator getSplitCalc() {
        return splitCalc;
    }

    public IndexPage[] getPages() {
        return pages;
    }

    public int entryCount() {
        return pageCount;
    }

    public String toString() {
        return "Internal page (" + min + ".." + max + "): " + Arrays.asList(pages);
    }

    public void lookup(Range range, LookupStats stats) {
        for (int i = 0; i < pageCount; i++) {
            if (pages[i].overlaps(range)) {
                pages[i].lookup(range, stats);
            }
        }
        stats.addStats(pageCount);
    }

    public Image makeImage(int imageWidth, int minImage, int maxImage, int depth) {
        int left = (int) (((long) min - minImage) * (imageWidth - 1) / (maxImage - minImage));
        int right = (int) (((long) max - minImage) * (imageWidth - 1) / (maxImage - minImage));
        if (right > imageWidth - 1) {
            right = imageWidth - 1;
        }
        if (left < 0) {
            left = 0;
        }
        int width = right - left + 1;
        //System.err.println("Internal index page " + left + ".." + right + " width " + width);
        int bgColour = (int) (colour & 0xffffff);

        List<Integer> occupied = new ArrayList<Integer>();
        List<int[]> pixels = new ArrayList<int[]>();

        IndexPage sortedPages[] = new IndexPage[pageCount];
        for (int i = 0; i < pageCount; i++) {
            sortedPages[i] = pages[i];
        }
        Arrays.sort(sortedPages);

        for (IndexPage page : sortedPages) {
            int row = -1;
            boolean needsDown = true;
            int pageLeft = (int) (((long) page.getMin() - minImage) * (imageWidth - 1) / (maxImage - minImage)) - left;
            int pageRight = (int) (((long) page.getMax() - minImage) * (imageWidth - 1) / (maxImage - minImage)) - left;
            //System.err.println("Page " + page.getMin() + ".." + page.getMax() + " pixels " + pageLeft + ".." + pageRight);
            if (pageLeft < 0) {
                pageLeft = 0;
            }
            if (pageRight > width - 1) {
                pageRight = width - 1;
            }
            if ((pageLeft <= width - 1) && (pageRight >= 0)) {
                if ((depth > 1) || ((depth < 0) && (page instanceof InternalIndexPage))) {
                    Image pageImage = page.makeImage(imageWidth, minImage, maxImage, depth - 1);
                    do {
                        row++;
                        boolean needsDown2 = false;
                        for (int i = row; i < row + pageImage.getHeight() + 2; i++) {
                            if (i >= occupied.size()) {
                                occupied.add(new Integer(0));
                                int[] pixelRow = new int[width];
                                for (int o = 0; o < width; o++) {
                                    pixelRow[o] = bgColour;
                                }
                                pixels.add(pixelRow);
                            }
                            if (occupied.get(i) > pageLeft) {
                                needsDown2 = true;
                            }
                        }
                        needsDown = needsDown && needsDown2;
                    } while (needsDown);
                    for (int y = 0; y < pageImage.getHeight(); y++) {
                        int[] rowToPaint = pixels.get(y + row + 1);
                        int[] rowFromPage = pageImage.getPixels().get(y);
                        for (int x = 0; x < pageImage.getWidth(); x++) {
                            rowToPaint[pageLeft + x] = rowFromPage[x];
                        }
                    }
                    for (int i = row ; i < row + pageImage.getHeight() + 2; i++) {
                        occupied.set(i, new Integer(pageRight + 1));
                    }
                } else {
                    do {
                        row++;
                        boolean needsDown2 = false;
                        for (int i = row; i < row + 3; i++) {
                            if (i >= occupied.size()) {
                                occupied.add(new Integer(0));
                                int[] pixelRow = new int[width];
                                for (int o = 0; o < width; o++) {
                                    pixelRow[o] = bgColour;
                                }
                                pixels.add(pixelRow);
                            }
                            if (occupied.get(i) > pageLeft) {
                                needsDown2 = true;
                            }
                        }
                        needsDown = needsDown && needsDown2;
                    } while (needsDown);
                    int[] rowToPaint = pixels.get(row + 1);
                    for (int x = pageLeft; x <= pageRight; x++) {
                        rowToPaint[x] = 0;
                    }
                    for (int i = row ; i < row + 3; i++) {
                        occupied.set(i, new Integer(pageRight + 2));
                    }
                }
            }
        }

        return new Image(left, pixels);
    }
}
