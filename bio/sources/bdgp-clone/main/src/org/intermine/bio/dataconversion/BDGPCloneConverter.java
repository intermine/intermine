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
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.MetaDataException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.dataconversion.ItemWriter;

import org.apache.log4j.Logger;

/**
 * DataConverter to load flat file linking BDGP clones to Flybase genes.
 * @author Richard Smith
 */
public class BDGPCloneConverter extends CDNACloneConverter
{
    protected static final Logger LOG = Logger.getLogger(BDGPCloneConverter.class);


    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public BDGPCloneConverter(ItemWriter writer)
        throws ObjectStoreException,
               MetaDataException {
        super(writer);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "BDGP");
        writer.store(ItemHelper.convert(dataSource));

        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "BDGP cDNA clone data set");
        writer.store(ItemHelper.convert(dataSet));

        organism = createItem("Organism");
        organism.setAttribute("taxonId", "7227");
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

            Item gene = createBioEntity("Gene", array[0], organism.getIdentifier());
            writer.store(ItemHelper.convert(gene));

            String[] cloneIds = array[3].split(";");

            for (int i = 0; i < cloneIds.length; i++) {
                Item clone = createBioEntity("CDNAClone", cloneIds[i], organism.getIdentifier());
                clone.setReference("gene", gene.getIdentifier());

                Item synonym = createItem("Synonym");
                synonym.setAttribute("type", "identifier");
                synonym.setAttribute("value", cloneIds[i]);
                synonym.setReference("source", dataSource.getIdentifier());
                synonym.setReference("subject", clone.getIdentifier());
                writer.store(ItemHelper.convert(synonym));

                clone.addCollection(new ReferenceList("evidence",
                    new ArrayList(Collections.singleton(dataSet.getIdentifier()))));
                writer.store(ItemHelper.convert(clone));
            }
        }
    }

}
