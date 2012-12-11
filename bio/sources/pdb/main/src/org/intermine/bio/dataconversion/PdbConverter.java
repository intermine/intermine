package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.biojava.bio.structure.DBRef;
import org.biojava.bio.structure.PDBHeader;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.io.PDBFileReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;


/**
 * @author Xavier Watkins
 *
 */
public class PdbConverter extends BioDirectoryConverter
{

    private static final Logger LOG = Logger.getLogger(PdbConverter.class);
    protected static final String ENDL = System.getProperty("line.separator");
    private Set<String> taxonIds = null;
    private Map<String, String> proteins = new HashMap<String, String>();

    /**
     * Create a new PdbConverter object.
     * @param writer the ItemWriter to store the objects in
     * @param model the Model
     */
    public PdbConverter(ItemWriter writer, Model model)  {
        super(writer, model, "PDB", "PDB dmel data set", null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(File dataDir) throws Exception {

        /**
         * if taxonids are specified, only process those directories.  otherwise
         * process them all.
         */

        File[] directories = dataDir.listFiles();
        List<File> directoriesToProcess = new ArrayList<File>();

        if (directories == null || directories.length == 0) {
            throw new RuntimeException("no valid PDB directories found");

        }

        for (File f : directories) {
            if (f.isDirectory()) {
                String directoryName = f.getName();
                if (taxonIds != null && !taxonIds.isEmpty()) {
                    if (taxonIds.contains(directoryName)) {
                        directoriesToProcess.add(f);
                    }
                } else {
                    directoriesToProcess.add(f);
                }
            }
        }

        // check that we have valid files before we start storing ANY data
        if (directoriesToProcess.isEmpty()) {
            throw new RuntimeException("no valid PDB directories found.");
        }

        // one dir per org
        for (File dir : directoriesToProcess) {
            String taxonId = dir.getName();
            File[] filesToProcess = dir.listFiles();
            proteins = new HashMap<String, String>();
            for (File f : filesToProcess) {
                if (f.getName().endsWith(".pdb")) {
                    processPDBFile(f, taxonId);
                }
            }
        }
    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setPdbOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + this.taxonIds);
    }

    private void processPDBFile(File file, String taxonId)
        throws Exception {
        PDBFileReader pdbreader = new PDBFileReader();
        Item proteinStructure = createItem("ProteinStructure");
        Structure structure = pdbreader.getStructure(file);
        PDBHeader header = structure.getPDBHeader(); 
        String idCode = header.getIdCode();
        if (StringUtils.isNotEmpty(idCode)) {
            proteinStructure.setAttribute("identifier", idCode);
        } else {
            throw new BuildException("No value for title in structure: " + idCode);
        }

        List<DBRef> dbrefs = structure.getDBRefs();
        for (DBRef dbref : dbrefs) {
            String proteinRefId = getProtein(dbref.getDbAccession(), taxonId);
            proteinStructure.addToCollection("proteins", proteinRefId);
        }

        String title = header.getTitle();
        if (StringUtils.isNotEmpty(title)) {
            proteinStructure.setAttribute("title", title);
        } else {
            LOG.warn("No value for title in structure: " + idCode);
        }
        String technique = header.getTechnique();
        if (StringUtils.isNotEmpty(technique)) {
            proteinStructure.setAttribute("technique", technique);
        } else {
            LOG.warn("No value for technique in structure: " + idCode);
        }
        
        String classification = header.getClassification();
        proteinStructure.setAttribute("classification", classification);
        
        final float resolutionFloat = header.getResolution();
        proteinStructure.setAttribute("resolution", String.valueOf(resolutionFloat));

        try {
            proteinStructure.setAttribute("atm", structure.toPDB());
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.error("Failed to process structure " + idCode);
        }
        store(proteinStructure);
    }

    private String getProtein(String accession, String taxonId)
        throws SAXException {
        String refId = proteins.get(accession);
        if (refId == null) {
            Item item = createItem("Protein");
            item.setAttribute("primaryAccession", accession);
            // TODO is there some way we can be certain of this taxonId for this protein?
            // item.setReference("organism", getOrganism(taxonId));
            refId = item.getIdentifier();
            proteins.put(accession, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return refId;
    }
}
