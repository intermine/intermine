package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;

/**
 *
 * @author
 */
public class SgdConverter extends BioDBConverter
{
    private static final Logger LOG = Logger.getLogger(SgdConverter.class);
    private static final String DATASET_TITLE = "SGD data set";
    private static final String DATA_SOURCE_NAME = "SGD";


    /**
     * Construct a new SgdConverter.
     * @param database the database to read from
     * @param model the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle Items created
     */
    public SgdConverter(Database database, ItemWriter writer, Model model) {
        super(database, model, writer, DATA_SOURCE_NAME, DATASET_TITLE);
    }


    /**
     * {@inheritDoc}
     */
    public void process() throws Exception {
        // a database has been initialised from properties starting with db.sgd

        Connection connection = getDatabase().getConnection();

        // process data with direct SQL queries on the source database, for example:

        // Statement stmt = connection.createStatement();
        // String query = "select column from table;";
        // ResultSet res = stmt.executeQuery(query);
        // while (res.next()) {
        // }



         Statement st = connection.createStatement();
         String q = "select * from FEATURE;";
         ResultSet r = st.executeQuery(q);
         int i = 0;
         while (r.next()) {

             /*
              * process sequences, genes, proteins, chomosomes, and CDSs
              */
             i++;
         }
         LOG.error("Processed " + i + " features");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int taxonId) {
        return DATASET_TITLE;
    }
}
