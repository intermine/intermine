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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * File processor processing references between genes and publications
 * from PubMed.
 * @author Jakub Kulaviak
 **/
public class ReferencesFileProcessor
{

    private BufferedReader reader;

    private Integer processedOrganism = null;

    /**
     * Constructor.
     * @param fileReader reader of file from which data are obtained, this class
     * is not responsible for closing fileReader
     */
    public ReferencesFileProcessor(Reader fileReader) {
        reader = new BufferedReader(fileReader);
    }

    /**
     * @return iterator over data in file. Each item in collection that is iterator
     * iterating over is PubMedReference object that carries information about
     * references between genes and publications for one organism.
     * @see PubMedReference for content of data
     */
    public Iterator<PubMedReference> getReferencesIterator() {
        return new ReferencesIterator();
    }

    private class ReferencesIterator implements Iterator<PubMedReference>
    {

        private int lineCounter = 0;

        private PubMedReference next = null;

        String lastLine = null;

        public ReferencesIterator() {
            try {
                next = parseNext();
            } catch (IOException ex) {
                throw  new ReferencesProcessorException(ex);
            }
        }

        private PubMedReference parseNext() throws IOException {
            if (reader == null) {
                return null;
            }
            String line;
            Map<Integer, List<Integer>> references = new HashMap<Integer, List<Integer>>();
            while ((line = getLine()) != null) {
                line = line.trim();
                lineCounter++;

                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }
                String[] parts = line.split("\\t");
                if (parts.length != 3) {
                    throw new ReferencesProcessorException("Error at " + lineCounter
                            + " line. It doesn't have"
                            + " format tax_id\tGeneID\tPubMed_ID.");
                }
                Integer organismId, geneId, pubId;
                try {
                    organismId = new Integer(parts[0].trim());
                    geneId = new Integer(parts[1].trim());
                    pubId = new Integer(parts[2].trim());
                } catch (NumberFormatException ex) {
                    throw new ReferencesProcessorException("Invalid identifier at line "
                            + lineCounter + ". Identifier is not integer.");
                }
                if (processedOrganism == null) {
                    processedOrganism = organismId;
                }
                if (organismId.intValue() == processedOrganism.intValue()) {
                    processReference(geneId, pubId, references);
                } else {
                    lastLine = line;
                    Integer retOrganism = processedOrganism;
                    processedOrganism = organismId;
                    return new PubMedReference(retOrganism, references);
                }
            }
            reader = null;
            return new PubMedReference(processedOrganism, references);
        }

        public boolean hasNext()  {
            return next != null;
        }

        private String getLine() throws IOException {
            if (lastLine != null) {
                String tmp = lastLine;
                lastLine = null;
                return tmp;
            }
            return reader.readLine();

        }

        private void processReference(Integer geneId, Integer pubId,
                Map<Integer, List<Integer>> references) {
            List<Integer> publications = references.get(geneId);
            if (publications == null) {
                publications = new ArrayList<Integer>();
                references.put(geneId, publications);
            }
            publications.add(pubId);
        }

        public PubMedReference next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            try {
                PubMedReference tmp = next;
                next = parseNext();
                return tmp;
            } catch (IOException e) {
                throw new  ReferencesProcessorException(e);
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
