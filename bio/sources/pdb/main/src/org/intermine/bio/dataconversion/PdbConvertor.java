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
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.biojava.bio.program.formats.Ligand.Compound;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.io.PDBFileParser;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * @author Xavier Watkins
 *
 */
public class PdbConvertor extends FileConverter
{

    private static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    private static final Logger LOG = Logger.getLogger(PdbConvertor.class);
    private String dataLocation;
    protected static final String ENDL = System.getProperty("line.separator");
    private Item dataSource, dataSet;
    
    /**
     * @param writer
     */
    public PdbConvertor(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);
        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "PDB");
        store(dataSource);

        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "PDB data");
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        store(dataSet);
    }
    
    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();
        if (currentFile.getName().endsWith(".pdb")) {
            Item proteinStructure = createItem("ProteinStructure");
           
            PDBFileParser pdbfileparser = new PDBFileParser();
            PdbBufferedReader pdbBuffReader = new PdbBufferedReader(reader);
            Structure structure = pdbfileparser.parsePDBFile(pdbBuffReader);
            String atm = structure.toPDB();
            
            proteinStructure.setAttribute("identifier", "pdb_" + getCurrentFile()
                                          .getName().substring(0,getCurrentFile().getName().lastIndexOf(".pdb")));
    
            List<String> proteins = new ArrayList<String>();
            List<String> dbrefs = pdbBuffReader.getDbrefs();
            for(String accnum: dbrefs) {
                Item protein = getAndStoreItemOnce("Protein", "primaryAccession", accnum);
                proteins.add(protein.getIdentifier());
            }
            
            proteinStructure.setAttribute("idCode", (String) structure.getHeader().get("idCode"));
            proteinStructure.setAttribute("title", (String) structure.getHeader().get("title"));
            proteinStructure.setAttribute("technique", (String) structure.getHeader().get("technique"));
            proteinStructure.setAttribute("classification", (String) structure.getHeader().get("classification"));
            proteinStructure.setAttribute("depDate", (String) structure.getHeader().get("depDate"));
            proteinStructure.setAttribute("modDate", (String) structure.getHeader().get("modDate"));
            proteinStructure.setAttribute("resolution", (String) structure.getHeader().get("resolution"));
            
            proteinStructure.setAttribute("atm", atm);
            proteinStructure.setCollection("proteins", proteins);
            store(proteinStructure);
        }
    }
    
    /**
     * Pick up the data location from the ant, the translator needs to open some more files.
     * @param srcdatadir location of the source data
     */
    public void setSrcDataDir(String srcdatadir) {
        this.dataLocation = srcdatadir;
    }
    
    /**
     * BioJava doesn't support getting DBREF so
     * we get it as the file is read.
     * 
     * @author Xavier Watkins
     *
     */
    public class PdbBufferedReader extends BufferedReader{

        private List<String> dbrefs = new ArrayList<String>();
        
        public PdbBufferedReader(Reader reader) {
            super(reader);
        }

        /* (non-Javadoc)
         * @see java.io.BufferedReader#readLine()
         */
        @Override
        public String readLine() throws IOException {
            String line = super.readLine();
            if (line != null && line.matches("^DBREF.*")) {
                String [] split = line.split("\\s+");
                if (split[5].equals("SWS")) {
                    dbrefs.add(split[6]);
                }
            }
            return line;
        }
        
        public List getDbrefs() {
            return dbrefs;
        }
        
        
    }

    
}