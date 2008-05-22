package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
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

/**
 *
 * @author Kim Rutherford
 */
public class FlyBaseDBConverter extends ChadoDBConverter
{

    /**
     * Create a new ChadoDBConverter object.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     */
    public FlyBaseDBConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(database, tgtModel, writer);
        // TODO Auto-generated constructor stub
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
        return "FlyBase " + genus + " " + species + " data set";
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
        String name = "FlyBase " + genus + " " + species + " data set";
        String description = "The FlyBase " + genus + " " + species + " genome";
        return getDataSetItem(name, "http://www.flybase.org", description,
                              getDataSourceItem());
    }



}
