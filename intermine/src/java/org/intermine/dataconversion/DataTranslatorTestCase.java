package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * TestCase for all DataTranslators
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public abstract class DataTranslatorTestCase extends TargetItemsTestCase
{
    /**
     * Get the Collection of test source Items
     * @return the Collection of Items
     * @throws Exception if an error occurs
     */
    protected abstract Collection getSrcItems() throws Exception;

    /**
     * Subclasses must provide access to the ontology model.
     * @return the ontology model
     */
    protected abstract OntModel getOwlModel();
}
