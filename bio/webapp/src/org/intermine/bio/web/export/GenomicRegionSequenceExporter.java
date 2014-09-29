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
import java.lang.NumberFormatException;

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
    private OutputStream out;
    
    private static final HashMap<String,String> pacIdHack;
    static {
        pacIdHack = new HashMap<String,String>();
        pacIdHack.put("195","A. coerulea");
        pacIdHack.put("107","A. lyrata");
        pacIdHack.put("167","A. thaliana");
        pacIdHack.put("192","B. distachyon");
        pacIdHack.put("277","B. rapa");
        pacIdHack.put("278","B. stricta");
        pacIdHack.put("182","C. clementina");
        pacIdHack.put("266","C. grandiflora");
        pacIdHack.put("113","C. papaya");
        pacIdHack.put("281","C. reinhardtii");
        pacIdHack.put("183","C. rubella");
        pacIdHack.put("122","C. sativus");
        pacIdHack.put("154","C. sinensis");
        pacIdHack.put("227","C. subellipsoidea C-169");
        pacIdHack.put("201","E. grandis");
        pacIdHack.put("173","E. salsugineum");
        pacIdHack.put("226","F. vesca");
        pacIdHack.put("275","G. max");
        pacIdHack.put("221","G. raimondii");
        pacIdHack.put("200","L. usitatissimum");
        pacIdHack.put("196","M. domestica");
        pacIdHack.put("147","M. esculenta");
        pacIdHack.put("256","M. guttatus");
        pacIdHack.put("228","M. pusilla CCMP1545");
        pacIdHack.put("229","M. sp. RCC299 ");
        pacIdHack.put("285","M. truncatula");
        pacIdHack.put("231","O. lucimarinus CCE9901");
        pacIdHack.put("204","O. sativa");
        pacIdHack.put("251","P. patens");
        pacIdHack.put("139","P. persica");
        pacIdHack.put("210","P. trichocarpa");
        pacIdHack.put("273","P. virgatum");
        pacIdHack.put("218","P. vulgaris");
        pacIdHack.put("119","R. communis");
        pacIdHack.put("255","S. bicolor");
        pacIdHack.put("164","S. italica");
        pacIdHack.put("225","S. lycopersicum");
        pacIdHack.put("91","S. moellendorffii");
        pacIdHack.put("206","S. tuberosum");
        pacIdHack.put("233","T. cacao");
        pacIdHack.put("199","V. carteri");
        pacIdHack.put("145","V. vinifera");
        pacIdHack.put("284","Z. mays");
        pacIdHack.put("264","A. halleri");
        pacIdHack.put("274","P. hallii");
        pacIdHack.put("283","B. distachyon");
    }

    // Map to hold DNA sequence of a whole chromosome in memory
    private static Map<MultiKey, String> chromosomeSequenceMap = new HashMap<MultiKey, String>();

    /**
     * Instructor
     *
     * @param os ObjectStore
     * @param response HttpServletResponse
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
    @SuppressWarnings("deprecation")
    public void export(List<GenomicRegion> grList) throws Exception {
        GenomicRegion aRegion = grList.get(0);
        Organism org = (Organism) DynamicUtil.createObject(Collections
                .singleton(Organism.class));
        // We normally specify the short name (text) as the key to the organism. A
        // numerical values for the organism say we're going to use another
        // key.
        try {
          org.setProteomeId(Integer.parseInt(aRegion.getOrganism()));
          org = (Organism) os.getObjectByExample(org, Collections.singleton("proteomeId"));
        } catch ( NumberFormatException e ) {
          org.setShortName(aRegion.getOrganism());
          org = (Organism) os.getObjectByExample(org, Collections.singleton("shortName"));
        }

        for (GenomicRegion gr : grList) {
            Chromosome chr = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
            chr.setPrimaryIdentifier(gr.getChr());
            chr.setOrganism(org);

            chr = (Chromosome) os.getObjectByExample(chr,
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
            headerBits.add(org.getShortName());
            String header = StringUtil.join(headerBits, " ");

            String seqName = "genomic_region_" + gr.getChr() + "_"
                    + start + "_" + end + "_"
                    + gr.getOrganism().replace("\\. ", "_");

            Sequence chrSeg = DNATools.createDNASequence(
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
