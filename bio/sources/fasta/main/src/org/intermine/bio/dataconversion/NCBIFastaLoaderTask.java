package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.Sequence;

/**
 * A loader that works for FASTA files with an NCBI formatted header:
 * http://www.ncbi.nlm.nih.gov/blast/fasta.shtml
 * http://en.wikipedia.org/wiki/Fasta_format
 * @author Kim Rutherford
 */
public class NCBIFastaLoaderTask extends FastaLoaderTask
{
    //protected static final Logger LOG = Logger.getLogger(NCBIFastaLoaderTask.class);
    private static final String ORG_HEADER = " Homo sapiens ";
    private static final String CHROMOSOME_HEADER = "chromosome";

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier(Sequence bioJavaSequence) {
        Annotation anno = bioJavaSequence.getAnnotation();
        String header = anno.getProperty("description_line").toString();
        // >gi|568815597|ref|NC_000001.11| Homo sapiens chromosome 1, GRCh38.p2 Primary Assembly
        // gi|251831106|ref|NC_012920.1| Homo sapiens mitochondrion, complete genome
        for (String headerString : header.split("\\|")) {
            if (headerString.contains("mitochondrion")) {
                return "MT";
            }
            // we want the phrase with "chromosome" in it
            if (headerString.contains(CHROMOSOME_HEADER)) {
                // chop off the part after the comma
                String[] headerSubStrings = headerString.split(",");
                // chop off everything but the chromosome number
                String identifier = headerSubStrings[0].substring(ORG_HEADER.length()
                        + CHROMOSOME_HEADER.length());
                return identifier.trim();

            }
        }
        // nothing found
        throw new RuntimeException("Couldn't find chromosome identifier " + header);
    }
}
