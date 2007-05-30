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

import java.io.Reader;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;

/**
 * @author Xavier Watkins
 *
 */
public class PdbConvertor extends FileConverter
{

    private static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private static final Logger LOG = Logger.getLogger(PdbConvertor.class);
    private ItemFactory itemFactory;
    private String dataLocation;
    protected static final String ENDL = System.getProperty("line.separator");
    
    /**
     * @param writer
     */
    public PdbConvertor(ItemWriter writer) {
        super(writer);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
    }

    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        Iterator fileIterator = TextFileUtil.parseTabDelimitedReader(reader);
        StringBuffer title = new StringBuffer();
        StringBuffer experiment = new StringBuffer();
        StringBuffer atm = new StringBuffer();
        StringBuffer keywords = new StringBuffer();
        while (fileIterator .hasNext()) {
            String [] lineArray = (String []) fileIterator .next();
            if (lineArray[0].equals("TITLE")) {
                title.append(lineArray[1]);
            } else if (lineArray[0].equals("KEYWDS")) {
                keywords.append(lineArray[1]);
            } else if (lineArray[0].equals("ATOM")) {
                atm.append(lineArray[1]);
            } else if (lineArray[0].equals("EXPDTA")) {
                experiment.append(lineArray[1]);
            } else if (lineArray[0].equals("DBREF")) {
                for (int i=1; i<lineArray.length ; i++) {
                    if (lineArray[i].equals("SWS")) {
                        String uniprotId = lineArray[i+1];
                        Item protein = createItem("Protein");
                        
                        break;
                    }
                }
                //look for SWS
            }
        }
        Item proteinStructure = createItem("ProteinStructure");
        proteinStructure
                        .setAttribute(
                                      "identifier",
                                      "pdb_"
                                                      + getCurrentFile()
                                                                        .getName()
                                                                        .substring(
                                                                                   0,
                                                                                   getCurrentFile()
                                                                                                   .getName()
                                                                                                   .lastIndexOf(
                                                                                                                ".pdb")));
        proteinStructure.setAttribute("keywords", keywords.toString());
        proteinStructure.setAttribute("atm", atm.toString());
        proteinStructure.setAttribute("title", title.toString());
        getItemWriter().store(ItemHelper.convert(proteinStructure));
    }
    
    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    private Item createItem(String className) {
        return itemFactory.makeItemForClass(GENOMIC_NS + className);
    }

}
