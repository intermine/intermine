import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class LookupStringSize
{
    public static final int SUPERSTRING_SIZE = 100000000;
    public static final int INDEX_SIZE = 100000;
    public static final int MAX_STRING_LENGTH = 10000000;

    public static void main(String args[]) {
        System.err.println("Generating superString");
        StringBuffer superStringBuffer = new StringBuffer(SUPERSTRING_SIZE);
        Random random = new Random();
        for (int i = 0; i < SUPERSTRING_SIZE; i++) {
            superStringBuffer.append((char) (random.nextInt(26) + 'a'));
        }
        String superString = superStringBuffer.toString();
        System.err.println("Finished generating superString");
        System.out.println("String length\tHash time\tHash present time\tTree time\tTree found time");
        for (int strLen = 2; strLen <= MAX_STRING_LENGTH; strLen += (strLen / 6) + 1) {
            Set<String> set = new HashSet<String>(INDEX_SIZE);
            for (int i = 0; i < INDEX_SIZE; i++) {
                int offset = random.nextInt(SUPERSTRING_SIZE - strLen);
                set.add(superString.substring(offset, offset + strLen));
            }
            List<String> toCheckNotFound = new ArrayList<String>(INDEX_SIZE);
            for (int i = 0; i < INDEX_SIZE; i++) {
                int offset = random.nextInt(SUPERSTRING_SIZE - strLen);
                toCheckNotFound.add(superString.substring(offset, offset + strLen));
            }
            List<String> toCheckFound = new ArrayList<String>(INDEX_SIZE);
            for (String value : set) {
                toCheckFound.add(new String(value));
            }
            System.gc();
            long startTime = System.currentTimeMillis();
            for (String value : toCheckNotFound) {
                set.contains(value);
            }
            long hashTime = System.currentTimeMillis() - startTime;
            startTime = System.currentTimeMillis();
            for (String value : toCheckFound) {
                set.contains(value);
            }
            long hashFoundTime = System.currentTimeMillis() - startTime;
            set = new TreeSet<String>();
            for (int i = 0; i < INDEX_SIZE; i++) {
                int offset = random.nextInt(SUPERSTRING_SIZE - strLen);
                set.add(superString.substring(offset, offset + strLen));
            }
            toCheckNotFound = new ArrayList<String>(INDEX_SIZE);
            for (int i = 0; i < INDEX_SIZE; i++) {
                int offset = random.nextInt(SUPERSTRING_SIZE - strLen);
                toCheckNotFound.add(superString.substring(offset, offset + strLen));
            }
            toCheckFound = new ArrayList<String>(INDEX_SIZE);
            for (String value : set) {
                toCheckFound.add(new String(value));
            }
            System.gc();
            startTime = System.currentTimeMillis();
            for (String value : toCheckNotFound) {
                set.contains(value);
            }
            long treeTime = System.currentTimeMillis() - startTime;
            startTime = System.currentTimeMillis();
            for (String value : toCheckFound) {
                set.contains(value);
            }
            long treeFoundTime = System.currentTimeMillis() - startTime;
            System.out.println(strLen + "\t" + hashTime + "\t" + hashFoundTime + "\t" + treeTime + "\t" + treeFoundTime);
        }
    }
}
