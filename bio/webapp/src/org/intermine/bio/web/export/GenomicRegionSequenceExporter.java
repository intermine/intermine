package org.intermine.bio.web.export;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;

/**
 * Exports DNA sequences of given genomic regions in FASTA format.
 *
 * @author Fengyuan Hu
 */
public class GenomicRegionSequenceExporter
{
    private ObjectStore os;
    private HttpServletResponse response;
    // Map to hold DNA sequence of a whole chromosome in memory
    private static Map<MultiKey, String> chromosomeSequenceMap = new HashMap<MultiKey, String>();

    /**
     * Instructor
     *
     * @param os ObjectStore
     * @param response HttpServletResponse
     */
    public GenomicRegionSequenceExporter(ObjectStore os, HttpServletResponse response) {
        this.os = os;
        this.response = response;
        this.response.setContentType("text/plain");
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

        try {
            org = (Organism) os.getObjectByExample(org,
                    Collections.singleton("shortName"));
        } catch (ObjectStoreException e) {
            throw new RuntimeException(
                    "Unable to fetch Organism object", e);
        }

        if (grList.isEmpty()) {
            PrintWriter pw = response.getWriter();
            pw.write("No sequence to export...");
            pw.flush();
        } else {
            OutputStream out = response.getOutputStream();
            for (GenomicRegion gr : grList) {
                Chromosome chr = (Chromosome) DynamicUtil
                .createObject(Collections.singleton(Chromosome.class));
                chr.setPrimaryIdentifier(gr.getChr());
                chr.setOrganism(org);

                try {
                    chr = (Chromosome) os.getObjectByExample(
                            chr,
                            new HashSet<String>(Arrays.asList(new String[] {
                                "primaryIdentifier", "organism" })));
                } catch (ObjectStoreException e) {
                    throw new RuntimeException(
                            "Unable to fetch Chromosome object", e);
                }

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

                Sequence chrSeg = DNATools
                        .createDNASequence(
                                chrResidueString.substring(start - 1, end),
                                seqName);
                chrSeg.getAnnotation().setProperty(
                        FastaFormat.PROPERTY_DESCRIPTIONLINE, header);

                // write it out
                SeqIOTools.writeFasta(out, chrSeg);
            }
            out.flush();
        }
    }
}
