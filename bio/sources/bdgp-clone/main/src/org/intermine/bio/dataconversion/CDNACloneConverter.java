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

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;

import org.apache.log4j.Logger;

/**
 * DataConverter to load flat file linking cdna clones to genes.
 * @author Wenyan Ji
 */
public abstract class CDNACloneConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    protected static final Logger LOG = Logger.getLogger(CDNACloneConverter.class);

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
    public CDNACloneConverter(ItemWriter writer)
        throws ObjectStoreException,
               MetaDataException {
        super(writer);

        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"), "-1_");

    }


    /**
     * Read each line from flat file.
     *
     * @see DataConverter#process
     */
    public abstract void process(Reader reader) throws Exception;

    /**
     * @param clsName = target class name
     * @param id = identifier
     * @param orgId = ref id for organism
     * @return item
     */
    protected Item createBioEntity(String clsName, String id, String orgId) {
        Item bioEntity = createItem(clsName);
        bioEntity.setAttribute("identifier", id);
        bioEntity.setReference("organism", orgId);
        return bioEntity;
    }

    /**
     * @param clsName = target class name
     * @return item created by itemFactory
     */
    protected Item createItem(String clsName) {
        return itemFactory.makeItemForClass(GENOMIC_NS + clsName);
    }

}


