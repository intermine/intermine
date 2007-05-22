package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;

/**
 * Task superclass for invoking converters.
 *
 * @author Matthew Wakeling
 */
public class ConverterTask extends DynamicAttributeTask
{
    private String modelName = null;
    private String osName;
    private String excludeList;

    /**
     * Set the objectstore name
     * @param osName the model name
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }

    /**
     * Sets the list of classes to NOT try and convert
     * @param excludeList the suitably formatted list of classs to exclude. 
     */
    public void setExcludeList(String excludeList) {
        this.excludeList = excludeList;
    }

    /**
     * Return the list set by setExcludeList().
     * @return the exclude list
     */
    public String getExcludeList() {
        return excludeList;
    }

    /**
     * Return the model name 
     * @return the Model name
     */
    public String getModelName() {
        return modelName;
    }
    
    /**
     * Set the target model name 
     * @param modelName the Model name
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    

    /**
     * Return the object store alias set by setOsName().
     * @return the object store alias
     */
    public String getOsName() {
        return osName;
    }

    /**
     * Runs various performance-enhancing SQL statements.
     *
     * @param os the ObjectStore on which to run the SQL
     * @throws SQLException if something goes wrong
     * @throws IOException if an error occurs while reading from the post-processing sql file
     */
    protected void doSQL(ObjectStore os) throws SQLException, IOException {
        if (os instanceof ObjectStoreInterMineImpl) {
            Connection c = null;
            try {
                c = ((ObjectStoreInterMineImpl) os).getConnection();
                Statement s = c.createStatement();
                System.err .println("ALTER TABLE reference ALTER refid SET STATISTICS 1000");
                s.execute("ALTER TABLE reference ALTER refid SET STATISTICS 1000");
                // TODO: files should be placed in resources
                String filename = getModelName() + "_src_items.sql";
                InputStream is = ConverterTask.class.getClassLoader().getResourceAsStream(filename);
                // .sql files not always being copied correctly
                if (is != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line = br.readLine();
                    while (line != null) {
                        s.execute(line);
                        System.err .println(line);
                        line = br.readLine();
                    }
                }
                System.err .println("ANALYSE");
                s.execute("ANALYSE");
            } finally {
                ((ObjectStoreInterMineImpl) os).releaseConnection(c);
            }
        }
    }
}

