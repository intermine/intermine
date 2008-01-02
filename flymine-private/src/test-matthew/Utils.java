import java.util.*;

public class Utils
{
    private static Random rand = new Random();
    private static long uniqueNumber = 0;

    public static String randString(int len)
    {
        String retVal = "";
        for (int i=0; i<len; i++)
        {
            retVal += (char) (rand.nextInt(26)+'a');
        }
        return retVal;
    }

    public static String uniqueString()
    {
        String retVal = "";
        long n = getUniqueNumber();
        do
        {
            retVal = ((char) ((n % 26) + 'a')) + retVal;
            n = n / 26;
        } while (n>0);
        return retVal;
    }

    public static synchronized long getUniqueNumber()
    {
        return uniqueNumber++;
    }
}
