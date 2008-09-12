import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;

public class SortXml
{
    public static final long MEGABYTES = 1000;

    public static void main(String args[]) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java SortXml <entry tag> <identifier tag>");
            System.err.println("Redirect input and output");
            System.exit(1);
        }
        String entryTag = args[0];
        String idTag = args[1];
        BufferedInputStream in = new BufferedInputStream(System.in);
        Block b = null;
        TreeSet<SortableBlock> sortableBlocks = new TreeSet<SortableBlock>();
        TreeSet<BlockIterator> blockSources = new TreeSet<BlockIterator>();
        long totalSize = 0;
        int blockCount = 0;
        long totalTotalSize = 0;
        int totalBlockCount = 0;
        File currentDir = new File(".");
        System.err.print("Reading data from input");
        do {
            b = getNextBlock(entryTag, idTag, in);
            //System.err.println("Got " + b);
            if (b instanceof SortableBlock) {
                SortableBlock sb = (SortableBlock) b;
                sortableBlocks.add(sb);
                blockCount++;
                totalBlockCount++;
                if (blockCount % 1000 == 0) {
                    System.err.print(".");
                    System.err.flush();
                }
                totalSize += sb.getData().length;
                totalTotalSize += sb.getData().length;
                if (totalSize > MEGABYTES * 1048576L) {
                    // Dump to file and start again.
                    System.err.print("\nDumping " + blockCount + " sorted blocks of size " + totalSize + " to temporary file");
                    File tempFile = File.createTempFile("xml", null, currentDir);
                    DataOutputStream tempFileOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));
                    int writtenBlocks = 0;
                    for (SortableBlock sb2 : sortableBlocks) {
                        byte[] idBytes = sb2.getId().getBytes();
                        tempFileOut.writeInt(idBytes.length);
                        tempFileOut.writeInt(sb2.getData().length);
                        tempFileOut.write(idBytes);
                        tempFileOut.write(sb2.getData());
                        writtenBlocks++;
                        if (writtenBlocks % 10000 == 0) {
                            System.err.print(".");
                            System.err.flush();
                        }
                    }
                    System.err.println("");
                    tempFileOut.writeInt(0);
                    tempFileOut.flush();
                    tempFileOut.close();
                    FileBlockIterator fbi = new FileBlockIterator(new DataInputStream(new BufferedInputStream(new FileInputStream(tempFile), 10485760)), tempFile);
                    blockSources.add(fbi);
                    sortableBlocks.clear();
                    totalSize = 0;
                    blockCount = 0;
                    System.err.print("Reading data from input");
                }
            } else {
                System.err.println("");
                if (blockCount > 0) {
                    System.err.println("Keeping " + blockCount + " sorted blocks of size " + totalSize + " in memory");
                }
                if ((b != null) || (totalBlockCount != 0)) {
                    System.err.println("Found " + b + " after " + totalBlockCount + " sorted blocks with size " + totalTotalSize);
                    System.err.print("Writing data to output");
                }
                if (!sortableBlocks.isEmpty()) {
                    blockSources.add(new TreeBlockIterator(sortableBlocks.iterator()));
                }
                int writtenBlocks = 0;
                while (!blockSources.isEmpty()) {
                    BlockIterator bi = blockSources.pollFirst();
                    System.out.write(bi.next().getData());
                    if (bi.hasNext()) {
                        blockSources.add(bi);
                    }
                    writtenBlocks++;
                    if (writtenBlocks % 10000 == 0) {
                        System.err.print(".");
                        System.err.flush();
                    }
                }
                System.err.println("");
                sortableBlocks.clear();
                blockSources.clear();
                totalSize = 0;
                blockCount = 0;
                totalTotalSize = 0;
                totalBlockCount = 0;
                if (b != null) {
                    System.out.write(b.getData());
                    System.err.print("Reading data from input");
                }
            }
        } while (b != null);
        System.out.flush();
        System.err.println("Finished");
    }

    public static Block getNextBlock(String entryTag, String idTag, InputStream in) throws IOException {
        int nextOpeningTag = findNextOpeningTag(entryTag, in);
        if (nextOpeningTag != 0) {
            return new UnsortableBlock(nextOpeningTag, in);
        }
        int nextClosingTag = findNextClosingTag(entryTag, in);
        if (nextClosingTag > 0) {
            return new SortableBlock(nextClosingTag, in, idTag);
        }
        return null;
    }

    public static int findNextOpeningTag(String tag, InputStream in) throws IOException {
        byte pattern[] = ("<" + tag + " ").getBytes();
        return findNext(pattern, in);
    }

    public static int findNextClosingTag(String tag, InputStream in) throws IOException {
        byte pattern[] = ("</" + tag + ">\n").getBytes();
        int index = findNext(pattern, in);
        if (index == 0) {
            return 0;
        }
        return index + pattern.length;
    }

    public static int findNext(byte pattern[], InputStream in) throws IOException {
        in.mark(10000000);
        boolean[] sofar = new boolean[pattern.length];
        for (int i = 0; i < sofar.length; i++) {
            sofar[i] = false;
        }
        int offset = 0;
        int chr = -1;
        do {
            chr = in.read();
            for (int i = sofar.length - 1; i > 0; i--) {
                sofar[i] = sofar[i - 1] && (pattern[i] == ((byte) chr));
            }
            sofar[0] = pattern[0] == ((byte) chr);
            offset++;
        } while ((!sofar[sofar.length - 1]) && (chr != -1));
        if (offset > 10000000) {
            System.err.println("\nFound pattern at excessive offset " + (offset - (chr == -1 ? 1 : pattern.length)));
        }
        in.reset();
        return offset - (chr == -1 ? 1 : pattern.length);
    }

    public static class Block {
        byte[] data;

        public byte[] getData() {
            return data;
        }
    }

    public static class UnsortableBlock extends Block {
        public UnsortableBlock(int byteCount, InputStream in) throws IOException {
            data = new byte[byteCount];
            in.read(data);
        }

        public String toString() {
            return "Unsortable block of " + data.length + " bytes";
        }
    }

    public static class SortableBlock extends Block implements Comparable<SortableBlock> {
        String id;

        public SortableBlock(int byteCount, InputStream in, String idTag) throws IOException {
            data = new byte[byteCount];
            in.read(data);
            String s = new String(data, 0, 500);
            int start = s.indexOf("<" + idTag + ">") + idTag.length() + 2;
            int end = s.indexOf("</" + idTag + ">");
            id = new String(s.substring(start, end));
        }

        public SortableBlock(String id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        public String getId() {
            return id;
        }

        public int compareTo(SortableBlock o) {
            return getId().compareTo(o.getId());
        }

        public String toString() {
            return "Sortable block of " + data.length + " bytes with id " + id;
        }
    }

    public static abstract class BlockIterator implements Iterator<SortableBlock>, Comparable<BlockIterator> {
        String nextId;
        SortableBlock nextBlock = null;

        public String getNextId() {
            return nextId;
        }

        public int compareTo(BlockIterator o) {
            return getNextId().compareTo(o.getNextId());
        }

        public boolean hasNext() {
            return nextId != null;
        }

        public SortableBlock next() {
            if (nextId == null) {
                throw new NoSuchElementException();
            }
            SortableBlock retval = nextBlock;
            nextBlock();
            return retval;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public abstract void nextBlock();
    }

    public static class TreeBlockIterator extends BlockIterator {
        Iterator<SortableBlock> iter;

        public TreeBlockIterator(Iterator<SortableBlock> iter) {
            this.iter = iter;
            nextBlock();
        }

        public void nextBlock() {
            if (iter.hasNext()) {
                nextBlock = iter.next();
                nextId = nextBlock.getId();
            } else {
                nextBlock = null;
                nextId = null;
            }
        }
    }

    public static class FileBlockIterator extends BlockIterator {
        DataInputStream file;
        File fileToDelete;

        public FileBlockIterator(DataInputStream file, File fileToDelete) {
            this.file = file;
            this.fileToDelete = fileToDelete;
            nextBlock();
        }

        public void nextBlock() {
            try {
                int nextIdLen = file.readInt();
                if (nextIdLen != 0) {
                    int nextDataLen = file.readInt();
                    byte[] idBytes = new byte[nextIdLen];
                    file.readFully(idBytes);
                    nextId = new String(idBytes);
                    byte[] data = new byte[nextDataLen];
                    file.readFully(data);
                    nextBlock = new SortableBlock(nextId, data);
                } else {
                    nextId = null;
                    nextBlock = null;
                    fileToDelete.delete();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
