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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.log4j.Logger;


/**
 * Class in charge of making sequence ontology objects
 *
 * @author Julie Sullivan
 */
public final class SequenceOntologyFactory
{
    private static final Logger LOG = Logger.getLogger(SequenceOntologyFactory.class);
    private static SequenceOntology so = null;

    private SequenceOntologyFactory() {
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
            String filename = null;
            InputStream oboFileStream = null;
            if (oboFile == null) {
                oboFileStream = SequenceOntologyFactory.class.getClassLoader()
                        .getResourceAsStream("so.obo");
            } else {
                try {
                    oboFileStream = new FileInputStream(oboFile);
                } catch (FileNotFoundException e) {
                    LOG.error("so.obo file not found");
                    return null;
                }
                filename = oboFile.getAbsolutePath();
            }
            InputStream termsFileStream = null;
            if (terms == null) {
                termsFileStream = SequenceOntologyFactory.class.getClassLoader()
                        .getResourceAsStream("so_terms-default");
            } else {
                try {
                    termsFileStream = new FileInputStream(terms);
                } catch (FileNotFoundException e) {
                    LOG.error("so_terms file not found");
                    return null;
                }
            }
            so = new SequenceOntology(oboFileStream, filename, termsFileStream);
        }
        return so;
    }

    /**
     * Remove instance, used for testing only
     */
    protected void reset() {
        so = null;
    }
}
