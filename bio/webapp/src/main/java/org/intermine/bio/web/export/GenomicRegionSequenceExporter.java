package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.biojava.nbio.core.sequence.AccessionID;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.io.FastaWriterHelper;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.metadata.StringUtil;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.DynamicUtil;

/**
 * Exports DNA sequences of given genomic regions in FASTA format.
 *
 * @author Fengyuan Hu
 */
public class GenomicRegionSequenceExporter
{
    private static final Logger LOG = Logger.getLogger(GenomicRegionSequenceExporter.class);
    private ObjectStore os;
    private OutputStream out;
    // Map to hold DNA sequence of a whole chromosome in memory
    private static Map<MultiKey, String> chromosomeSequenceMap = new HashMap<MultiKey, String>();

    /**
     * Instructor
     *
     * @param os ObjectStore
     * @param out output stream
     */
    public GenomicRegionSequenceExporter(ObjectStore os, OutputStream out) {
        this.os = os;
        this.out = out;
    }

    /**
     * DO export
     * @param grList a list of GenomicRegion objects
     * @throws Exception ex
     */
    public void export(List<GenomicRegion> grList) throws Exception {
        GenomicRegion aRegion = grList.get(0);
        Organism org = (Organism) DynamicUtil.createObject(Collections
                .singleton(Organism.class));
        org.setShortName(aRegion.getOrganism());

        org = os.getObjectByExample(org, Collections.singleton("shortName"));

        for (GenomicRegion gr : grList) {
            Chromosome chr = (Chromosome) DynamicUtil.createObject(
                    Collections.singleton(Chromosome.class));
            chr.setPrimaryIdentifier(gr.getChr());
            chr.setOrganism(org);

            chr = os.getObjectByExample(chr,
                        new HashSet<String>(Arrays.asList("primaryIdentifier", "organism")));

            String chrResidueString;
            if (chromosomeSequenceMap.get(new MultiKey(gr.getChr(), gr
                    .getOrganism())) == null) {
                chrResidueString = chr.getSequence().getResidues()
                        .toString();
                chromosomeSequenceMap.put(
                        new MultiKey(gr.getChr(), gr.getOrganism()), chr
                                .getSequence().getResidues().toString());
            } else {
                chrResidueString = chromosomeSequenceMap.get(new MultiKey(
                        gr.getChr(), gr.getOrganism()));
            }

            int chrLength = chr.getLength();
            int start;
            int end;

            if (gr.getExtendedRegionSize() > 0) {
                start = gr.getExtendedStart();
                end = gr.getExtendedEnd();
            } else {
                start = gr.getStart();
                end = gr.getEnd();
            }

            end = Math.min(end, chrLength);
            start = Math.max(start, 1);

            List<String> headerBits = new ArrayList<String>();
            headerBits.add(gr.getChr() + ":" + start + ".." + end);
            headerBits.add(end - start + 1 + "bp");
            headerBits.add(gr.getOrganism());
            String header = StringUtil.join(headerBits, " ");
            String seqName = "genomic_region_" + gr.getChr() + "_"
                    + start + "_" + end + "_"
                    + gr.getOrganism().replace("\\. ", "_");

            DNASequence chrSeg = new DNASequence(
                    chrResidueString.substring(start - 1, end).toLowerCase());
            chrSeg.setAccession(new AccessionID(header.toString()));
            FastaWriterHelper.writeSequence(out, chrSeg);

        }
        out.flush();
    }
}
