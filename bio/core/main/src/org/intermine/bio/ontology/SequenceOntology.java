package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;


/**
 * Class representing the Sequence Ontology for this Mine.
 *
 * @author Julie Sullivan
 */
public final class SequenceOntology
{
    private static final Logger LOG = Logger.getLogger(SequenceOntology.class);
    private static SequenceOntology so = null;
    private static Model soModel = null;

    private SequenceOntology() {
        //disable external instantiation
    }

    /**
     * @return Sequence ontology object for this mine
     */
    public static SequenceOntology getSequenceOntology() {
        return getSequenceOntology(null, null);
    }

    /**
     * Given an OBO file, create a Sequence Ontology object
     *
     * @param oboFile so.obo OBO file for SO terms.  if null, will try to use the one in the
     * resources directory
     * @param terms file lists the terms to include in this mine.  If null, default values will
     * be used
     * @return sequence ontology object
     */
    public static SequenceOntology getSequenceOntology(File oboFile, File terms) {
        if (so == null) {
            so = new SequenceOntology();
            if (oboFile == null) {
                try {
                    oboFile = new File(
                            SequenceOntology.class.getClassLoader().getResource("so.obo").toURI());
                } catch (URISyntaxException e) {
                    LOG.error("Could not process so.obo file");
                    return null;
                }
            }
            if (terms == null) {
                try {
                    terms = new File(SequenceOntology.class.getClassLoader()
                                    .getResource("so_terms-default").toURI());
                } catch (URISyntaxException e) {
                    LOG.error("Could not process so_terms-modMine file");
                    return null;
                }
            }

            // new parser to filter on terms
            SequenceOntologyParser parser = new SequenceOntologyParser(oboFile, terms);

            // given the OBO file and the terms to filter on, return valid model
            soModel = parser.getModel();
        }
        return so;
    }

    /**
     * For a given so term, return all parents in order.  term must be a valid SO term, eg. gene
     *
     * @param className name of child class
     * @return list of parents in order
     */
    public List<ClassDescriptor> getParents(String className) {
        return soModel.getBottomUpLevelTraversal();
    }

    /**
     * Gets the model containing only classes in the sequence ontology.  Used to write so_additions
     * which is merged into the main intermine model.
     *
     * @return the model
     */
    public Model getModel() {
        return soModel;
    }

    /**
     * Remove instance, used for testing only
     */
    protected void reset() {
        so = null;
    }
}
