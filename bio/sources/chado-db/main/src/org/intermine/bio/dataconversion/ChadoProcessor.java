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

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;

import java.sql.Connection;

/**
 * A proccesor for a chado module.  See http://www.gmod.org/wiki/index.php/Chado#Modules for
 * documentation about the possible modules.
 * @author Kim Rutherford
 */
public abstract class ChadoProcessor
{
    private final ChadoDBConverter chadoDBConverter;

    /**
     * Create a new ChadoModuleProcessor object.
     * @param chadoDBConverter the converter that created this Processor
     */
    public ChadoProcessor(ChadoDBConverter chadoDBConverter) {
        this.chadoDBConverter = chadoDBConverter;
    }

    /**
     * Return the ChadoDBConverter that was passed to the constructor.
     * @return the chadoDBConverter
     */
    public final ChadoDBConverter getChadoDBConverter() {
        return chadoDBConverter;
    }

    /**
     * Return the database to read from
     * @return the database
     */
    public Database getDatabase() {
        return chadoDBConverter.getDatabase();
    }

    /**
     * Return an ItemWriter used to handle the resultant Items
     * @return the writer
     */
    public ItemWriter getItemWriter() {
        return chadoDBConverter.getItemWriter();
    }

    /**
     * Return the Model of the target database.
     * @return the model
     */
    public Model getModel() {
        return chadoDBConverter.getModel();
    }

    /**
     * Do the processing for this module - called by ChadoDBConverter.
     * @param connection the database connection to chado
     * @throws Exception if the is a problem while processing
     */
    public abstract void process(Connection connection) throws Exception;
}