package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Helper class for data converters
 * @author julie
  */
public class BioConverterUtil
{
    private static Map<String, String> javaNamesToSO = new HashMap<String, String>();
    /**
     * Get SO name for Java class, eg. SequenceFeature to sequence_feature
     * @param javaClassName the relevant string
     * @return the SO value
     * @throws IOException if something goes wrong
     */
    public static String javaNameToSO(String javaClassName) throws IOException  {
        if (javaNamesToSO.isEmpty()) {
            String fileName = "/WEB-INF/soClassName.properties";
            File f = new File(fileName);
            if (f.exists()) {
                Reader reader = new FileReader(f);
                BufferedReader br = new BufferedReader(reader);
                // don't load header
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    String fields[] = StringUtils.split(line, ' ');
                    String javaName = fields[0];
                    String soName = fields[1];
                    javaNamesToSO.put(javaName, soName);
                }
            }
        }
        return javaNamesToSO.get(javaClassName);
    }
}
