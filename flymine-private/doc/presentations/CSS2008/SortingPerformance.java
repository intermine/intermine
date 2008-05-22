
import java.util.*;

/**
 * A class to demonstrate and measure the performance of different algorithms.
 *
 * @author Matthew Wakeling
 */
public class SortingPerformance
{
    public static final int DATA_SIZE[] = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 40, 70,
        100, 200, 400, 700, 1000, 2000, 4000, 7000, 10000, 20000, 40000, 70000, 100000, 200000,
        400000, 700000, 1000000, 2000000, 4000000, 7000000, 10000000, 20000000, 40000000, 70000000,
        100000000};
    public static final int REPEATS[] = new int[] {8388608, 8388608, 8388608, 4194304, 4194304,
        4194304, 2097152, 2097152, 2097152, 1048576, 1048576, 524288, 262144, 262144, 131072,
        65536, 32768, 16384, 8192, 2046, 512, 128, 32, 16, 4, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1};
    public static final int TEST_SIZE = 30000;
    public static final int BUBBLE_SORT = 0;
    public static final int BUBBLE_SORT_IMPROVED = 1;
    public static final int INSERT_SORT = 2;
    public static final int ARRAYS_DOT_SORT = 3;
    public static final int SHELL_SORT = 4;
    public static final int SELECTION_SORT = 5;
    public static final int QUICKSORT = 6;
    public static final int QUICKSORT_HYBRID = 7;
    public static final int ALGO_COUNT = 8;

    public static void main(String args[]) {
        int testControl[] = new int[TEST_SIZE];
        int testSampleBubble[] = new int[TEST_SIZE];
        int testSampleBubbleImproved[] = new int[TEST_SIZE];
        int testSampleInsert[] = new int[TEST_SIZE];
        int testSampleShell[] = new int[TEST_SIZE];
        int testSampleSelection[] = new int[TEST_SIZE];
        int testSampleQuickSort[] = new int[TEST_SIZE];
        int testSampleQuickSortHybrid[] = new int[TEST_SIZE];
        Random random = new Random();
        for (int i = 0; i < TEST_SIZE; i++) {
            testControl[i] = random.nextInt();
            testSampleBubble[i] = testControl[i];
            testSampleBubbleImproved[i] = testControl[i];
            testSampleInsert[i] = testControl[i];
            testSampleShell[i] = testControl[i];
            testSampleSelection[i] = testControl[i];
            testSampleQuickSort[i] = testControl[i];
            testSampleQuickSortHybrid[i] = testControl[i];
        }
        Arrays.sort(testControl);
        bubbleSort(testSampleBubble);
        bubbleSortImproved(testSampleBubbleImproved);
        insertionSort(testSampleInsert);
        shellSort(testSampleShell);
        selectionSort(testSampleSelection);
        quickSort(testSampleQuickSort, 0, TEST_SIZE);
        quickSortHybrid(testSampleQuickSortHybrid, 0, TEST_SIZE);
        for (int i = 0; i < TEST_SIZE; i++) {
            if (testSampleBubble[i] != testControl[i]) {
                throw new IllegalArgumentException("Bubble sort is broken: expected " + testControl[i] + " but was " + testSampleBubble[i]);
            }
            if (testSampleBubbleImproved[i] != testControl[i]) {
                throw new IllegalArgumentException("Improved bubble sort is broken: expected " + testControl[i] + " but was " + testSampleBubbleImproved[i]);
            }
            if (testSampleInsert[i] != testControl[i]) {
                throw new IllegalArgumentException("Insertion sort is broken: expected " + testControl[i] + " but was " + testSampleInsert[i]);
            }
            if (testSampleShell[i] != testControl[i]) {
                throw new IllegalArgumentException("Shell sort is broken: expected " + testControl[i] + " but was " + testSampleShell[i]);
            }
            if (testSampleSelection[i] != testControl[i]) {
                throw new IllegalArgumentException("Selection sort is broken: expected " + testControl[i] + " but was " + testSampleSelection[i]);
            }
            if (testSampleQuickSort[i] != testControl[i]) {
                throw new IllegalArgumentException("QuickSort is broken: expected " + testControl[i] + " but was " + testSampleQuickSort[i]);
            }
            if (testSampleQuickSortHybrid[i] != testControl[i]) {
                throw new IllegalArgumentException("QuickSorti Hybrid is broken: expected " + testControl[i] + " but was " + testSampleQuickSortHybrid[i]);
            }
        }
        boolean alive[] = new boolean[ALGO_COUNT];
        for (int i = 0; i < ALGO_COUNT; i++) {
            alive[i] = true;
        }
        System.out.println("Data size\tRepeats\tBubble sort\tBubble sort improved\tInsertion sort\tArrays.sort\tShell sort\tSelection sort\tQuickSort\tQuickSort Hybrid");
        for(int i = 0; i < DATA_SIZE.length; i++) {
            System.out.print(DATA_SIZE[i] + "\t" + REPEATS[i]);
            for(int sortType = 0; sortType < ALGO_COUNT; sortType++) {
                if (alive[sortType]) {
                    MeasuredResult r = testSort(sortType, DATA_SIZE[i], REPEATS[i]);
                    System.out.print("\t" + r.getElapsedTime());
                    if ((r.getElapsedTime() / REPEATS[i]) > 4000) {
                        alive[sortType] = false;
                    }
                } else {
                    System.out.print("\t");
                }
            }
            System.out.println("");
        }
    }

    private static class MeasuredResult
    {
        private String algorithm;
        private int dataSize, repeatCount;
        private long elapsedTime;

        public MeasuredResult(String algorithm, int dataSize, int repeatCount, long elapsedTime) {
            this.algorithm = algorithm;
            this.dataSize = dataSize;
            this.repeatCount = repeatCount;
            this.elapsedTime = elapsedTime;
        }

        public String toString() {
            return algorithm + "\t" + dataSize + "\t" + repeatCount + "\t" + elapsedTime;
        }

        public long getElapsedTime() {
            return elapsedTime;
        }
    }

    public static MeasuredResult testSort(int sortType, int dataSize, int repeatCount) {
        int data[][] = new int[repeatCount][];
        Random random = new Random();
        for (int repeat = 0; repeat < repeatCount; repeat++) {
            data[repeat] = new int[dataSize];
            for (int i = 0; i < dataSize; i++) {
                data[repeat][i] = random.nextInt();
            }
        }
        System.gc();
        long startTime = System.currentTimeMillis();
        if (sortType == BUBBLE_SORT) {
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                int a[] = data[repeat];
                bubbleSort(a);
            }
            return new MeasuredResult("Bubble sort", dataSize, repeatCount,
                    System.currentTimeMillis() - startTime);
        } else if (sortType == BUBBLE_SORT_IMPROVED) {
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                int a[] = data[repeat];
                bubbleSortImproved(a);
            }
            return new MeasuredResult("Bubble sort improved", dataSize, repeatCount,
                    System.currentTimeMillis() - startTime);
        } else if (sortType == INSERT_SORT) {
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                int a[] = data[repeat];
                insertionSort(a);
            }
            return new MeasuredResult("Insertion sort", dataSize, repeatCount,
                    System.currentTimeMillis() - startTime);
        } else if (sortType == ARRAYS_DOT_SORT) {
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                int a[] = data[repeat];
                Arrays.sort(a);
            }
            return new MeasuredResult("Arrays.sort", dataSize, repeatCount,
                    System.currentTimeMillis() - startTime);
        } else if (sortType == SHELL_SORT) {
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                int a[] = data[repeat];
                shellSort(a);
            }
            return new MeasuredResult("Shell sort", dataSize, repeatCount,
                    System.currentTimeMillis() - startTime);
        } else if (sortType == SELECTION_SORT) {
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                int a[] = data[repeat];
                selectionSort(a);
            }
            return new MeasuredResult("Selection sort", dataSize, repeatCount,
                    System.currentTimeMillis() - startTime);
        } else if (sortType == QUICKSORT) {
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                int a[] = data[repeat];
                quickSort(a, 0, a.length);
            }
            return new MeasuredResult("Quicksort", dataSize, repeatCount,
                    System.currentTimeMillis() - startTime);
        } else if (sortType == QUICKSORT_HYBRID) {
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                int a[] = data[repeat];
                quickSortHybrid(a, 0, a.length);
            }
            return new MeasuredResult("Quicksort Hybrid", dataSize, repeatCount,
                    System.currentTimeMillis() - startTime);
        } else {
            throw new IllegalArgumentException("No such algorithm " + sortType);
        }
    }

    private static void bubbleSort(int a[]) {
        boolean swapped;
        do {
            swapped = false;
            for (int i = 0; i < a.length - 1; i++) {
                if (a[i] > a[i + 1]) {
                    int swap = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = swap;
                    swapped = true;
                }
            }
        } while (swapped);
    }

    private static void bubbleSortImproved(int a[]) {
        boolean swapped;
        int last = a.length - 1;
        do {
            swapped = false;
            for (int i = 0; i < last; i++) {
                if (a[i] > a[i + 1]) {
                    int swap = a[i];
                    a[i] = a[i + 1];
                    a[i + 1] = swap;
                    swapped = true;
                }
            }
            last--;
        } while (swapped);
    }

    private static void insertionSort(int a[]) {
        for (int i = 1; i < a.length; i++) {
            int swap = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > swap) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = swap;
        }
    }

    private static void shellSort(int a[]) {
        for (int increment = a.length / 2; increment > 0;
                increment = (increment == 2 ? 1 : (int) Math.round(increment / 2.2))) {
            for (int i = increment; i < a.length; i++) {
                int temp = a[i];
                int j = i;
                while (j >= increment && a[j - increment] > temp) {
                    a[j] = a[j - increment];
                    j -= increment;
                }
                a[j] = temp;
            }
        }
    }

    private static void selectionSort(int a[]) {
        for (int i = 0; i < a.length - 1; i++) {
            int minPos = i;
            for (int j = i + 1; j < a.length; j++) {
                if (a[j] < a[minPos]) { 
                    minPos = j;
                }
            }
            int swap = a[minPos];
            a[minPos] = a[i];
            a[i] = swap;
        }
    }

    private static void quickSort(int a[], int start, int end) {
        if (start < end - 1) {
            int left = start;
            int right = end - 2;
            while (left < right + 1) {
                while (left < right + 1 && a[left] <= a[end - 1]) {
                    left++;
                }
                while (left < right + 1 && a[right] > a[end - 1]) {
                    right--;
                }
                if (left < right) {
                    int swap = a[left];
                    a[left] = a[right];
                    a[right] = swap;
                }
            }
            int swap = a[left];
            a[left] = a[end - 1];
            a[end - 1] = swap;
            quickSort(a, start, left);
            quickSort(a, left + 1, end);
        }
    }

    private static void quickSortHybrid(int a[], int start, int end) {
        if (start < end - 7) {
            if (start < end - 100) {
                // Find the most suitable pivot.
            }
            int left = start;
            int right = end - 2;
            while (left < right + 1) {
                while (left < right + 1 && a[left] <= a[end - 1]) {
                    left++;
                }
                while (left < right + 1 && a[right] > a[end - 1]) {
                    right--;
                }
                if (left < right) {
                    int swap = a[left];
                    a[left] = a[right];
                    a[right] = swap;
                }
            }
            int swap = a[left];
            a[left] = a[end - 1];
            a[end - 1] = swap;
            quickSortHybrid(a, start, left);
            quickSortHybrid(a, left + 1, end);
        } else {
            // Insertion sort instead.
            for (int i = start + 1; i < end; i++) {
                int swap = a[i];
                int j = i - 1;
                while (j >= start && a[j] > swap) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = swap;
            }
        }
    }
}
