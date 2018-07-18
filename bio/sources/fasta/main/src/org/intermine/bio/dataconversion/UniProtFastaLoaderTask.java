package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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

import org.apache.log4j.Logger;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Code for loading fasta for uniprot proteins.
 * @author julie
 */
public class UniProtFastaLoaderTask extends FastaLoaderTask
{
    protected static final Logger LOG = Logger.getLogger(FastaLoaderTask.class);
    private Map<Integer, Organism> organisms = new HashMap<Integer, Organism>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected Organism getOrganism(ProteinSequence bioJavaSequence) throws ObjectStoreException {
        String header = bioJavaSequence.getOriginalHeader();
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
