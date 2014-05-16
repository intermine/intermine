package org.intermine.bio.like;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author selma
 *
 */
public class Storing {

    private Map<Coordinates, Integer> row;

    private Storing() {
        // Don't.
    }

    /**
     *
     * @param matrixRow containing the gene ID at its relationship to all other genes
     * @param geneID Gene ID, which shall be stored
     * @throws IOException matrixRow is not serializable
     */
    public static void storeRow(Map<Coordinates, Integer> matrixRow, Integer geneID)
        throws IOException {
        File rowIn = new File(geneID.toString());
        FileOutputStream f = new FileOutputStream(rowIn);
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(matrixRow);
        s.close();
    }

    /**
     *
     * @param geneID Gene ID, which is wanted
     * @return geneIDs relationships to all other genes
     * @throws IOException
     * @throws ClassNotFoundException file called "geneID" can't be found
     */
    public Map<Coordinates, Integer> getRow(Integer geneID)
        throws IOException, ClassNotFoundException {
        File rowOut = new File(geneID.toString());
        FileInputStream f2 = new FileInputStream(rowOut);
        ObjectInputStream s2 = new ObjectInputStream(f2);
        row = (HashMap<Coordinates, Integer>) s2.readObject();
        s2.close();
        return row;
    }
}
