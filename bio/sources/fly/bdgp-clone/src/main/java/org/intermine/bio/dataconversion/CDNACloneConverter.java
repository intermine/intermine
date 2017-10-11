package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * DataConverter to load flat file linking cdna clones to genes.
 * @author Wenyan Ji
 */
public abstract class CDNACloneConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(CDNACloneConverter.class);

    protected Item organism;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @param dataSourceName the DataSource name
     * @param dataSetTitle the DataSet title
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public CDNACloneConverter(ItemWriter writer, Model model,
                              String dataSourceName, String dataSetTitle)
        throws ObjectStoreException,
               MetaDataException {
        super(writer, model, dataSourceName, dataSetTitle);
    }

    /**
     * Read each line from flat file.
     *
     * {@inheritDoc}
     */
    public abstract void process(Reader reader) throws Exception;

    /**
     * Create and return a new BioEntity.
     * @param clsName target class name
     * @param id identifier to set attrName to
     * @param attrName the attrName to use for the identifier
     * @param orgId ref id for organism
     * @return the new Item
     */
    protected Item createBioEntity(String clsName, String id, String attrName, String orgId) {
        Item bioEntity = createItem(clsName);
        bioEntity.setAttribute(attrName, id);
        bioEntity.setReference("organism", orgId);
        return bioEntity;
    }
}
