package org.intermine.bio.dataconversion;

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
import java.io.InputStreamReader;

import org.intermine.InterMineException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;

/**
 * @author Xavier Watkins
 *
 */
public class MockProteinStructureDataConvertor extends ProteinStructureDataConvertor
{
    public MockProteinStructureDataConvertor(ItemWriter writer) {
        super(writer, Model.getInstanceByName("genomic"));
    }
    
    protected String getFileContent(String fileName, String extention) throws InterMineException {
        String str;
        StringBuffer atm = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getClassLoader()
                                                                                        .getResourceAsStream(fileName)));
            
            boolean firstLine = true;
            while ((str = in.readLine()) != null) {
                if (!firstLine ) {
                    atm.append(ENDL);
                }    
                atm.append(str);
                firstLine = false;
            }
            in.close();
         }
         catch (IOException e) {
            throw new InterMineException(e);
        }
        return atm.toString();
    }

}
