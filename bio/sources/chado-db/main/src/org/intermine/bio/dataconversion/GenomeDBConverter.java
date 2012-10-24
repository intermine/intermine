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

import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.sql.Database;

import java.sql.SQLException;

/**
 * A ChadoDBConverter that sets the dataset and datasource for genome databases (eg. WormBase and
 * FlyBase).
 * @author Kim Rutherford
 */
public class GenomeDBConverter extends ChadoDBConverter
{

    /**
     * Create a new ChadoDBConverter object.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     * @throws SQLException if we fail to get a database connection
     */
    public GenomeDBConverter(Database database, Model tgtModel, ItemWriter writer)
        throws SQLException {
        super(database, tgtModel, writer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int taxonId) {
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        OrganismData od = or.getOrganismDataByTaxon(taxonId);
        String species = od.getSpecies();
        String genus = od.getGenus();
        return getDataSourceName() + " " + genus + " " + species + " data set";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Item getDataSetItem(int taxonId) {
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        OrganismData od = or.getOrganismDataByTaxon(taxonId);
        String species = od.getSpecies();
        String genus = od.getGenus();
        String name = getDataSourceName() + " " + genus + " " + species + " data set";
        String description = "The " + getDataSourceName() + " " + genus + " " + species + " genome";
        String url = "http://www." + getDataSourceName().toLowerCase() + ".org";
        return getDataSetItem(name, url, description,
                              getDataSourceItem());
    }



}
