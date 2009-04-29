import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InternalIndexPage extends IndexPage
{
    private int maxPages, pageCount;
    private IndexPage[] pages;
    private PenaltyCalculator penaltyCalc;
    private SplitCalculator splitCalc;

    public InternalIndexPage(int maxPages, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc) {
        this.maxPages = maxPages;
        this.penaltyCalc = penaltyCalc;
        this.splitCalc = splitCalc;
        pages = new IndexPage[maxPages + 1];
        pageCount = 0;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
    }

    public InternalIndexPage(int maxPages, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc,
            SplitPage splitPage) {
        this(maxPages, penaltyCalc, splitCalc);
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
        min = Math.min(min, entry.getMin());
        max = Math.max(max, entry.getMax());
        try {
            pages[bestPage].addEntry(entry);
        } catch (PageNeedsSplitException e) {
            SplitPage splitPage = splitCalc.calc(pages[bestPage]);
            pages[bestPage] = splitPage.getLeft();
            pages[pageCount++] = splitPage.getRight();
            if (pageCount > maxPages) {
                throw new PageNeedsSplitException();
            }
        }
    }

    protected void addPage(IndexPage page) {
        pages[pageCount++] = page;
        min = Math.min(min, page.getMin());
        max = Math.max(max, page.getMax());
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

    public String toString() {
        return "Internal page (" + min + ".." + max + "): " + Arrays.asList(pages);
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
        int bgColour = hashCode() & 0xffffff;

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
                if (depth > 1) {
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
                    for (int x = pageLeft; x < pageRight; x++) {
                        rowToPaint[x] = 0;
                    }
                    for (int i = row ; i < row + 3; i++) {
                        occupied.set(i, new Integer(pageRight + 1));
                    }
                }
            }
        }

        return new Image(left, pixels);
    }
}
