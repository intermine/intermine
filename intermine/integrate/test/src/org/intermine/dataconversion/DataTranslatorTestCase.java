package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;

import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;

/**
 * TestCase for all DataTranslators
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Peter McLaren - adding some logger related code.
 */
public abstract class DataTranslatorTestCase extends TargetItemsTestCase
{
    protected Model srcModel;
    protected Properties mapping;

    /**
     * Create a new DataTranslatorTestCase object.
     * @param arg the argument to pass the to super constructor
     */
    public DataTranslatorTestCase(String arg, String oswAlias) {
        super(arg, oswAlias);
    }

    /**
     * @see TargetItemsTestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        mapping = new Properties();
        InputStream is =
            getClass().getClassLoader().getResourceAsStream(getSrcModelName() + "_mappings");
        if (is != null) {
            mapping.load(is);
        }
        srcModel = Model.getInstanceByName(getSrcModelName());

    }

    /**
     * Get the target Model for this test.
     * @param ns the namespace for the target model
     * @return the target Model
     * @throws MetaDataException if the Model cannot be found
     */
    public Model getTargetModel(String ns) throws MetaDataException {
        if (ns.equals("http://www.flymine.org/model/genomic#")) {
            return Model.getInstanceByName("genomic");
        }

        throw new RuntimeException("can't find Model for: " + ns);
    }

    /**
     * Get the Collection of test source Items
     * @return the Collection of Items
     * @throws Exception if an error occurs
     */
    protected abstract Collection getSrcItems() throws Exception;


    /**
     * Get the source Model for this test.
     * @return the source Model
     */
    protected abstract String getSrcModelName();

}

