import java.util.Random;

public class StorageHierarchy
{
    public static final int BUF_SIZE = 512 * 1024 * 1024;
    public static final int ITERS = 30000000;

    public static void main(String args[]) {
        int buffer[] = new int[BUF_SIZE];

        int p = 0;
        Random r = new Random();

        long start = System.currentTimeMillis();
        for (int o = 0; o < ITERS; o++) {
            p += r.nextInt(3);
        }
        System.out.println("Calibrate:\t" + (System.currentTimeMillis() - start));

        for (int i = 2; i<= BUF_SIZE; i += (i / 6) + 1) {
            start = System.currentTimeMillis();
            for (int o = 0; o < ITERS; o++) {
                p += buffer[r.nextInt(i)];
            }
            System.out.println(i + "\t" + (System.currentTimeMillis() - start));
        }
    }

}
