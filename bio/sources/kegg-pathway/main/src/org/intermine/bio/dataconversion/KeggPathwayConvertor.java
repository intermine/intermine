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

import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;


/**
 * DataConverter to load Kegg Pathways and link them to Genes
 *
 * @author Xavier Watkins
 */
public class KeggPathwayConvertor extends FileConverter
{
    protected Item dataSource, dataSet;
    protected Map<String, String> keggOrganismToTaxonId = new HashMap<String, String>();
    protected HashMap pathwayMap = new HashMap();
    private String dataLocation;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public KeggPathwayConvertor(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model);

        // Drosophila melanogaster
        keggOrganismToTaxonId.put("dme", "7227");
        // Drosophila pseudoobscura
//        keggOrganismToTaxonId.put("dpo", "7237");
        // Anopheles gambiae
//        keggOrganismToTaxonId.put("aga", "180454");
        // Apis mellifera
//        keggOrganismToTaxonId.put("dame", "7460");

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Kyoto Encyclopedia of Genes and Genomes");
        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "KEGG PATHWAY - dme");
        dataSet.setAttribute("url", "http://www.genome.jp/kegg/pathway.html");
        store(dataSource);
        store(dataSet);
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = TextFileUtil.parseTabDelimitedReader(reader);

        // there a two files
        // data is in format
        // CG | list of space separated map Id's
        // and
        // Map Id | name

        File currentFile = getCurrentFile();

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            Pattern filePattern = Pattern.compile("^(\\S+)_gene_map.*");
            Matcher matcher = filePattern.matcher(currentFile.getName());
            if (line.length <= 1 || line[0].startsWith("#")) {
                continue;
            }
            if (currentFile.getName().startsWith("map_title")) {
                String mapIdentifier = line[0];
                String mapName = line[1];
                Item pathway = getAndStoreItemOnce("Pathway","identifier", mapIdentifier);
                pathway.setAttribute("name", mapName);
                pathway.setCollection("evidence", new ArrayList(Collections
                                                                .singleton(dataSet.getIdentifier())));
                store(pathway);
            } else if (matcher.find()){
                String keggOrgName = matcher.group(1);
                String taxonId = keggOrganismToTaxonId.get(keggOrgName);

                if (taxonId != null && taxonId.length() !=0 ) {
                    Item organism = getAndStoreItemOnce("Organism", "taxonId", taxonId);

                    String geneName = line[0];

                    // There are a couple of Transcripts ID's so for the moment we don't want them
                    if (geneName.startsWith("Dmel_")) {
                        geneName = geneName.substring(5);
                    } else {
                        continue;
                    }
                    String mapIdentifiers = line[1];
                    ReferenceList referenceList = new ReferenceList("pathways");
                    String [] mapArray = mapIdentifiers.split(" ");
                    for (int i = 0; i < mapArray.length; i++) {
                        referenceList.addRefId(getAndStoreItemOnce("Pathway","identifier", mapArray[i])
                                               .getIdentifier());
                    }
                    Item gene = createItem("Gene");
                    gene.setAttribute("identifier", geneName);
                    gene.setReference("organism", organism);

                    gene.addCollection(referenceList);
                    store(gene);
                }
            }
        }
    }

    /**
     * Pick up the data location from the ant, the translator needs to open some more files.
     * @param srcdatadir location of the source data
     */
    public void setSrcDataDir(String srcdatadir) {
        this.dataLocation = srcdatadir;
    }
}
