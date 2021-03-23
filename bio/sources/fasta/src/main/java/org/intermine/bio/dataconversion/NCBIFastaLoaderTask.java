package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.template.Sequence;

/**
 * A loader that works for FASTA files with an NCBI formatted header:
 * http://www.ncbi.nlm.nih.gov/blast/fasta.shtml
 * http://en.wikipedia.org/wiki/Fasta_format
 * @author Kim Rutherford
 */
public class NCBIFastaLoaderTask extends FastaLoaderTask
{
    protected static final Logger LOG = Logger.getLogger(NCBIFastaLoaderTask.class);
    private static final String ORG_HEADER = " Homo sapiens ";
    private static final String CHROMOSOME_HEADER = "chromosome";

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier(Sequence bioJavaSequence) {

        String header = ((DNASequence) bioJavaSequence).getOriginalHeader();
        // >ref|NC_000001.11| Homo sapiens chromosome 1, GRCh38.p12 Primary Assembly
        // >ref|NC_012920.1| Homo sapiens mitochondrion, complete genome

        // new header:
        // >NC_000024.10 Homo sapiens chromosome Y, GRCh38.p13 Primary Assembly


        for (String headerString : header.split("\\|")) {
            if (headerString.contains("mitochondrion")) {
                return "MT";
            }
            // we want the phrase with "chromosome" in it
            if (headerString.contains(CHROMOSOME_HEADER)) {
                // chop off the part after the comma
                String[] headerSubStrings = headerString.split(",");
                // chop off everything but the chromosome number

                String[] lastHeader = headerSubStrings[0].split(CHROMOSOME_HEADER);
                return lastHeader[1].trim();
            }
        }
        // nothing found
        throw new RuntimeException("Couldn't find chromosome identifier " + header);
    }
}
