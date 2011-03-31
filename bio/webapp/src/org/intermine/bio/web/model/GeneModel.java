package org.intermine.bio.web.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.util.DynamicUtil;

public class GeneModel {
    Model model;
    Gene gene;
    InterMineObject transcript;
    InterMineObject threePrimeUTR;
    InterMineObject fivePrimeUTR;
    List<InterMineObject> exons;
    List<InterMineObject> introns;
    List<InterMineObject> cdss;

    protected static final Logger LOG = Logger.getLogger(GeneModel.class);


    public GeneModel(Model model, InterMineObject transcript) {
        this.transcript = transcript;
        this.model = model;
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        // find other components
        if (transcript == null) {
            return;
        }
        if (!classExists("Transcript")) {
            return;
        }

        // TODO sort exons and introns by start position
        if (fieldExists("Transcript", "exons")) {
            exons = new ArrayList<InterMineObject>();
            try {
                Collection<InterMineObject> transcriptExons =
                    (Collection<InterMineObject>) transcript.getFieldValue("exons");
                for (InterMineObject exon : transcriptExons) {
                    exons.add(exon);
                }
            } catch (IllegalAccessException e) {
                LOG.warn("Failed to fetch exons for transcript: " + transcript.getId());
            }
        }

        if (fieldExists("Transcript", "introns")) {
            introns = new ArrayList<InterMineObject>();
            try {
                Collection<InterMineObject> transcriptIntrons =
                    (Collection<InterMineObject>) transcript.getFieldValue("introns");
                for (InterMineObject intron : transcriptIntrons) {
                    introns.add(intron);
                }
            } catch (IllegalAccessException e) {
                LOG.warn("Failed to fetch introns for transcript: " + transcript.getId());
            }
        }

        if (fieldExists("Transcript", "CDSs")) {
            cdss = new ArrayList<InterMineObject>();
            try {
                Collection<InterMineObject> transcriptCdss =
                    (Collection<InterMineObject>) transcript.getFieldValue("CDSs");
                for (InterMineObject cds : transcriptCdss) {
                    cdss.add(cds);
                }
            } catch (IllegalAccessException e) {
                LOG.warn("Failed to fetch CDSs for transcript: " + transcript.getId());
            }
        }

        if (fieldExists("Transcript", "UTRs")) {
            try {
                Collection<InterMineObject> transcriptUTRs =
                    (Collection<InterMineObject>) transcript.getFieldValue("UTRs");
                for (InterMineObject utr : transcriptUTRs) {
                    String clsName = DynamicUtil.getSimpleClass(utr).getSimpleName();
                    if ("FivePrimeUTR".equals(clsName)) {
                        if (fivePrimeUTR != null) {
                            LOG.warn("More than one five prime UTR found for transcript: "
                                    + transcript.getId());
                        } else {
                            fivePrimeUTR = utr;
                        }
                    }
                    if ("ThreePrimeUTR".equals(clsName)) {
                        if (threePrimeUTR != null) {
                            LOG.warn("More than one three prime UTR found for transcript: "
                                    + transcript.getId());
                        } else {
                            threePrimeUTR = utr;
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                LOG.warn("Failed to fetch UTRs for transcript: " + transcript.getId());
            }
        }

    }

    public InterMineObject getTranscript() {
        return transcript;
    }


    public List<InterMineObject> getExons() {
        return exons;
    }

    public List<InterMineObject> getIntrons() {
        return introns;
    }

    public List<InterMineObject> getCDSs() {
        return cdss;
    }
    public InterMineObject getThreePrimeUTR() {
        return threePrimeUTR;
    }

    public InterMineObject getFivePrimeUTR() {
        return fivePrimeUTR;
    }

    private boolean classExists(String clsName) {
        ClassDescriptor cld = model.getClassDescriptorByName(clsName);
        return !(cld == null);
    }

    private boolean fieldExists(String clsName, String fieldName) {
        ClassDescriptor cld = model.getClassDescriptorByName(clsName);
        FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
        return !(fd == null);
    }

}
