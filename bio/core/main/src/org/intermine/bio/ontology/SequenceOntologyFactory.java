package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
    private static final String OBO_FILE = "so-obo";
    private static File tmpFile = null;

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
                try {
                    filename = writeFile(oboFileStream);
                } catch (IOException e) {
                    LOG.error("so.obo file not found");
                    return null;
                }
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
            so = new SequenceOntology(filename, termsFileStream);
        }
        return so;
    }

    private static String writeFile(InputStream oboFileStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(oboFileStream));
        if (tmpFile == null || !tmpFile.exists()) {
            tmpFile = File.createTempFile(OBO_FILE, ".tmp");
            FileWriter writer = new FileWriter(tmpFile);
            String line = null;
            while ((line = reader.readLine()) != null) {
                writer.write(line + "\n");
            }
            writer.flush();
            writer.close();
        }
        return tmpFile.getAbsolutePath();
    }

    /**
     * Remove instance, used for testing only
     */
    protected void reset() {
        so = null;
    }
}
