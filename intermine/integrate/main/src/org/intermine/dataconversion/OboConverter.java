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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.ontology.DagTerm;
import org.intermine.ontology.DagTermSynonym;
import org.intermine.ontology.OboParser;
import org.intermine.ontology.OboTerm;
import org.intermine.ontology.OboTermSynonym;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;

/**
 * Convert tree of OboTerms into Items.
 * 
 * @author Thomas Riley
 * @see DagConverter
 */
public class OboConverter extends DagConverter
{
    /**
     * Construct a new instance of OboConverter.
     * @see DagConverter#DagConverter(ItemWriter, String, String, String, String)
     */
    public OboConverter(ItemWriter writer, String dagFilename, String dagName, String url,
            String termClass) {
        super(writer, dagFilename, dagName, url, termClass);
    }

    /**
     * Cast the DagTerm to an OboTerm and set the namespace, description and obsolete attributes.
     * 
     * @see DagConverter#configureItem(String, Item, DagTerm)
     */
    protected void configureItem(String termId, Item item, DagTerm term)
        throws ObjectStoreException {
        super.configureItem(termId, item, term);
        OboTerm oboterm = (OboTerm) term;
        item.addAttribute(new Attribute("namespace", oboterm.getNamespace()));
        item.addAttribute(new Attribute("description", oboterm.getDescription()));
        item.addAttribute(new Attribute("obsolete", "" + oboterm.isObsolete()));
    }

    /**
     * Cast DagTermSynonym to OboTermSynonym and set the type attribute on the
     * synonym item.
     * 
     * @see DagConverter#configureSynonymItem(DagTermSynonym, Item, DagTerm)
     */
    protected void configureSynonymItem(DagTermSynonym syn, Item item, DagTerm term)
        throws ObjectStoreException {
        super.configureSynonymItem(syn, item, term);
        OboTermSynonym osyn = (OboTermSynonym) syn;
        item.addAttribute(new Attribute("type", osyn.getType()));
    }
    
    /**
     * Get all root terms for an OBO format file.
     * @param oboFile the OBO file
     * @return Collection of root OboTerms
     * @throws IOException if something goes wrong
     */
    protected Collection findRootTerms(File oboFile) throws IOException {
        return new OboParser().processForLabellingOntology(new FileReader(oboFile));
    }
}
