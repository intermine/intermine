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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;

/**
 * Helper class for data converters
 * @author julie
  */
public class BioConverterUtil
{
    private static final String PROP_FILE = "soClassName.properties";
    private static Map<String, String> javaNamesToSO = new HashMap<String, String>();
    /**
     * Get SO name for Java class, eg. SequenceFeature to sequence_feature
     * @param javaClassName the relevant string
     * @return the SO value
     * @throws IOException if something goes wrong
     */
    public static String javaNameToSO(String javaClassName) throws IOException  {
        if (javaNamesToSO.isEmpty()) {
            InputStream is = BioConverterUtil.class.getClassLoader().getResourceAsStream(PROP_FILE);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(is));
            } catch (Exception e) {
                throw new BuildException("cannot file SO file: " + PROP_FILE, e);
            }
            String line = null;
            while ((line = br.readLine()) != null) {
                String fields[] = StringUtils.split(line, ' ');
                String javaName = fields[0];
                String soName = fields[1];
                javaNamesToSO.put(javaName, soName);
            }
        }
        return javaNamesToSO.get(javaClassName);
    }
}
