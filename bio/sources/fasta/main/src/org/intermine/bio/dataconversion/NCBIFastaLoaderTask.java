package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
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
    private static final String CHROMOSOME_HEADER = " Homo sapiens chromosome ";

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier(Sequence bioJavaSequence) {
        Annotation anno = bioJavaSequence.getAnnotation();
        String header = anno.getProperty("description_line").toString();
        // >gi|568815597|ref|NC_000001.11| Homo sapiens chromosome 1, GRCh38.p2 Primary Assembly
        String[] bits = header.split("\\|");
        for (String bit : bits) {
            if (bit.contains("chromosome")) {
                String[] furtherBits = bit.split(",");
                for (String anotherString : furtherBits) {
                    if (anotherString.startsWith(CHROMOSOME_HEADER)) {
                        String identifier = anotherString.substring(CHROMOSOME_HEADER.length());
                        return identifier;
                    }
                }
            }
        }
        // nothing found
        throw new RuntimeException("Couldn't find chromosome identifier " + header);
    }
}
