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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.Sequence;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Code for loading fasta for uniprot proteins.
 * @author julie
 */
public class UniProtFastaLoaderTask extends FastaLoaderTask
{
    private Map<Integer, Organism> organisms = new HashMap<Integer, Organism>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected Organism getOrganism(Sequence bioJavaSequence) throws ObjectStoreException {
        Annotation anno = bioJavaSequence.getAnnotation();
        //description_line=sp|Q9V8R9-2|41_DROME Isoform 2 of Protein 4.1 homolog OS=Drosophila
        // melanogaster GN=cora,
        String header = anno.getProperty("description_line").toString();
        final String regexp = "OS\\=\\w+\\s\\w+";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(header);
        if (m.find()) {
            header = m.group();
            String[] bits = header.split("=");
            if (bits.length != 2) {
                return null;
            }
            Integer taxonId = getTaxonId(bits[1]);
            if (taxonId == null) {
                return null;
            }
            Organism org = organisms.get(taxonId);
            if (org == null) {
                org = getDirectDataLoader().createObject(Organism.class);
                org.setTaxonId(taxonId);
                getDirectDataLoader().store(org);
                organisms.put(taxonId, org);
            }
            return org;
        }
        return null;
    }
}
