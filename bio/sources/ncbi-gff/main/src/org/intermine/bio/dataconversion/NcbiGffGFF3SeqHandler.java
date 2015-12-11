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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.xml.full.Item;

/**
 * A handler for NCBI GFF chromosomes
 *
 * @author Julie
 */
public class NcbiGffGFF3SeqHandler extends GFF3SeqHandler
{
    protected static final Logger LOG = Logger.getLogger(NcbiGffGFF3SeqHandler.class);
    private static final Map<String, String> CHROMOSOMES = new LinkedHashMap<String, String>();
    private Set<String> resolvedIds = new HashSet<String>();

    /**
     * Construct the seq handler.
     */
    public NcbiGffGFF3SeqHandler() {
        // nothing
    }

    static {
        CHROMOSOMES.put("NC_000001.11", "1");
        CHROMOSOMES.put("NC_000002.12", "2");
        CHROMOSOMES.put("NC_000003.12", "3");
        CHROMOSOMES.put("NC_000004.12", "4");
        CHROMOSOMES.put("NC_000005.10", "5");
        CHROMOSOMES.put("NC_000006.12", "6");
        CHROMOSOMES.put("NC_000007.14", "7");
        CHROMOSOMES.put("NC_000008.11", "8");
        CHROMOSOMES.put("NC_000009.12", "9");
        CHROMOSOMES.put("NC_000010.11", "10");

        CHROMOSOMES.put("NC_000011.10", "11");
        CHROMOSOMES.put("NC_000012.12", "12");
        CHROMOSOMES.put("NC_000013.11", "13");
        CHROMOSOMES.put("NC_000014.9", "14");
        CHROMOSOMES.put("NC_000015.10", "15");
        CHROMOSOMES.put("NC_000016.10", "16");
        CHROMOSOMES.put("NC_000017.11", "17");
        CHROMOSOMES.put("NC_000018.10", "18");
        CHROMOSOMES.put("NC_000019.10", "19");
        CHROMOSOMES.put("NC_000020.11", "20");

        CHROMOSOMES.put("NC_000021.9", "21");
        CHROMOSOMES.put("NC_000022.11", "22");
        CHROMOSOMES.put("NC_000023.11", "X");
        CHROMOSOMES.put("NC_000024.10", "Y");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSeqIdentifier(String id) {
        if (resolvedIds.contains(id)) {
            // we've already seen and resolved this ID
            return id;
        }

        String resolvedIdentifier = CHROMOSOMES.get(id);
        if (StringUtils.isNotEmpty(resolvedIdentifier)) {
            // SUCCESS add to IDs we've seen
            resolvedIds.add(resolvedIdentifier);
        }
        return resolvedIdentifier;
    }
}
