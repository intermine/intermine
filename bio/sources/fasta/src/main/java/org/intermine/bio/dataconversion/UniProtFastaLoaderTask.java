package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2022 FlyMine
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
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;
import org.apache.commons.lang.StringUtils;

/**
 * Code for loading fasta for uniprot proteins.
 * @author julie
 */
public class UniProtFastaLoaderTask extends FastaLoaderTask
{
    private Map<String, Organism> organisms = new HashMap<String, Organism>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected Organism getOrganism(ProteinSequence bioJavaSequence) throws ObjectStoreException {
        String header = bioJavaSequence.getOriginalHeader();
        final String regexp = "OS\\=(\\w+(\\s)*)*";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(header);
        if (m.find()) {
            header = m.group();
            header = cleanHeader(header);
            String[] bits = header.split("=");
            if (bits.length != 2) {
                return null;
            }
            String taxonId = getTaxonId(bits[1]);
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

    /**
     * the header might end with OX, GN, PE or SV (due to the new regular expression)
     * Example: OS=Severe acute respiratory syndrome coronavirus 2 OX
     *
     * @param header a partial header
     * @return the header without suffix
     */
    private String cleanHeader(String header) {
        String[] suffixes = {" OX", " GN", " PE", " SV"};
        for (String suffix : suffixes) {
            if (StringUtils.endsWith(header, suffix)) {
                return StringUtils.removeEnd(header, suffix);
            }
        }
        return header;
    }
}
