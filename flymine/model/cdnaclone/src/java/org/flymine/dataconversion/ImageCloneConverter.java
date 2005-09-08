package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;

import org.apache.log4j.Logger;

/**
 * DataConverter to load flat file linking BDGP clones to Flybase genes.
 * @author Wenyan Ji
 */
public class ImageCloneConverter extends CDNACloneConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected static final Logger LOG = Logger.getLogger(ImageCloneConverter.class);

    protected Item dataSource;
    protected Item dataSet;
    protected Item organism;
    protected ItemFactory itemFactory;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public ImageCloneConverter(ItemWriter writer) throws ObjectStoreException,
                                                        MetaDataException {
        super(writer);

        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"), "-1_");

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "RZPD");
        writer.store(ItemHelper.convert(dataSource));

        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "RZPD uniprot data set");
        writer.store(ItemHelper.convert(dataSet));
 
        organism = createItem("Organism");
        organism.setAttribute("abbreviation", "HS");
        writer.store(ItemHelper.convert(organism));
    }


    /**
     * Read each line from flat file.
     *
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {

        BufferedReader br = new BufferedReader(reader);
        //intentionally throw away first line
        String line = br.readLine();

        while ((line = br.readLine()) != null) {
            String[] array = line.split("\t", -1); //keep trailing empty Strings

            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }

            String geneId = array[16].trim();
            Item gene = createGene("Gene", geneId, organism.getIdentifier());
            writer.store(ItemHelper.convert(gene));

            String cloneId = array[5];

            Item clone = createBioEntity("CDNAClone", cloneId, organism.getIdentifier());
            clone.setReference("gene", gene.getIdentifier());

            Item synonym = createItem("Synonym");
            synonym.setAttribute("type", "identifier");
            synonym.setAttribute("value", cloneId);
            synonym.setReference("source", dataSource.getIdentifier());
            synonym.setReference("subject", clone.getIdentifier());
            writer.store(ItemHelper.convert(synonym));

            clone.addCollection(new ReferenceList("evidence",
                    new ArrayList(Collections.singleton(dataSet.getIdentifier()))));
            writer.store(ItemHelper.convert(clone));
        }
    }

    private Item createGene(String clsName, String id, String orgId) {
        Item gene = createItem(clsName);
        gene.setAttribute("organismDbId", id);
        gene.setReference("organism", orgId);
        return gene;
    }

}


