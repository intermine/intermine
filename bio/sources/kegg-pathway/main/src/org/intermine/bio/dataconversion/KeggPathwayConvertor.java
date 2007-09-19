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
import java.util.HashMap;
import java.util.Iterator;

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
    protected Item dataSource, dmel;
    protected HashMap pathwayMap = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public KeggPathwayConvertor(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Kegg");
        store(dataSource);

        dmel = createItem("Organism");
        dmel.setAttribute("taxonId", "7227");
        store(dmel);

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
            if (line.length <= 1 || line[0].startsWith("#")) {
                continue;
            }
            if (currentFile.getName().startsWith("map_title")) {
                String mapIdentifier = line[0];
                String mapName = line[1];
                Item pathway = getPathway(mapIdentifier);
                pathway.setAttribute("name", mapName);
                store(pathway);
            } else if (currentFile.getName().startsWith("dme_gene_map")) {
                String geneName = line[0];
                if (geneName.startsWith("Dmel_")) {
                    geneName = geneName.substring(5);
                } else {
                    continue;
                }
                String mapIdentifiers = line[1];
                ReferenceList referenceList = new ReferenceList("pathways");
                String [] mapArray = mapIdentifiers.split(" ");
                for (int i = 0; i < mapArray.length; i++) {
                    referenceList.addRefId(getPathway(mapArray[i]).getIdentifier());
                }
                Item gene = createItem("Gene");
                gene.setAttribute("identifier", geneName);
                gene.setReference("organism", dmel);

                gene.addCollection(referenceList);
                store(gene);
            }
        }
    }

    private Item getPathway(String identifier) {
        Item pathway = (Item) pathwayMap.get(identifier);
        if (pathway == null) {
            pathway = createItem("Pathway");
            pathway.setAttribute("identifier", "dme" + identifier);
            pathwayMap.put(identifier, pathway);
        }
        return pathway;
    }
}
