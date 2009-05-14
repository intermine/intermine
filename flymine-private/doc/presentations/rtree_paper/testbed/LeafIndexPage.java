import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LeafIndexPage extends IndexPage
{
    private int maxEntries, entryCount;
    private IndexEntry[] entries;

    public LeafIndexPage(long colour, int maxEntries) {
        super(colour);
        this.maxEntries = maxEntries;
        entries = new IndexEntry[maxEntries + 1];
        entryCount = 0;
    }

    public void addEntry(IndexEntry entry) throws PageNeedsSplitException {
        entries[entryCount++] = entry;
        expandToCover(entry);
        if (entryCount > maxEntries) {
            throw new PageNeedsSplitException();
        }
    }

    public void addEntryWithoutCheck(IndexEntry entry) {
        entries[entryCount++] = entry;
        expandToCover(entry);
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public IndexEntry[] getEntries() {
        return entries;
    }

    public int entryCount() {
        return entryCount;
    }

    public String toString() {
        return "Leaf page (" + min + ".." + max + "): " + Arrays.asList(entries);
    }

    public void lookup(Range range, LookupStats stats) {
        int results = 0;
        for (int i = 0; i < entryCount; i++) {
            if (entries[i].overlaps(range)) {
                results++;
            }
        }
        stats.addStats(entryCount, results);
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
        //System.err.println("Leaf index page " + left + ".." + right + " width " + width);
        int bgColour = (int) (colour & 0xffffff);

        List<Integer> occupied = new ArrayList<Integer>();
        List<int[]> pixels = new ArrayList<int[]>();

        IndexEntry sortedEntries[] = new IndexEntry[entryCount];
        for (int i = 0; i < entryCount; i++) {
            sortedEntries[i] = entries[i];
        }
        Arrays.sort(sortedEntries);

        for (IndexEntry entry : sortedEntries) {
            int row = -1;
            boolean needsDown = true;
            int entryLeft = (int) (((long) entry.getMin() - minImage) * (imageWidth - 1) / (maxImage - minImage)) - left;
            int entryRight = (int) (((long) entry.getMax() - minImage) * (imageWidth - 1) / (maxImage - minImage)) - left;
            //System.err.println("Entry " + entry.getMin() + ".." + entry.getMax() + " pixels " + entryLeft + ".." + entryRight);
            if (entryLeft < 0) {
                entryLeft = 0;
            }
            if (entryRight > width - 1) {
                entryRight = width - 1;
            }
            if ((entryLeft <= width - 1) && (entryRight >= 0)) {
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
                        if (occupied.get(i) > entryLeft) {
                            needsDown2 = true;
                        }
                    }
                    needsDown = needsDown && needsDown2;
                } while (needsDown);
                int[] rowToPaint = pixels.get(row + 1);
                for (int i = entryLeft; i <= entryRight; i++) {
                    rowToPaint[i] = 0;
                }
                occupied.set(row, new Integer(entryRight + 2));
                occupied.set(row + 1, new Integer(entryRight + 2));
                occupied.set(row + 2, new Integer(entryRight + 2));
            }
        }

        return new Image(left, pixels);
    }
}
