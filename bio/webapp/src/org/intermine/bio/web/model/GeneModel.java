package org.intermine.bio.web.model;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.util.DynamicUtil;

/**
 * Representation of a gene model - being one transcript of a gene and including exons, introns,
 * UTRs and CDSs where data are available.  On construction for a particular transcript this will
 * fetch additional information from the database.
 * @author Richard Smith
 *
 */
public class GeneModel
{
    Model model;
    Gene gene;
    InterMineObject transcript;
    InterMineObject threePrimeUTR;
    InterMineObject fivePrimeUTR;
    List<InterMineObject> exons;
    List<InterMineObject> introns;
    List<InterMineObject> cdss;
    Set<Integer> ids = null;

    private static String[] types = new String[] {"Gene", "Transcript", "Exon", "UTR", "CDS"};
    /**
     * The unqualified class names of types that comprise a gene model.
     */
    public static final Set<String> TYPES = new HashSet<String>(Arrays.asList(types));

    protected static final Logger LOG = Logger.getLogger(GeneModel.class);

    /**
     * Construct a new gene model for the given transcript and gene.  This will run queries for
     * exons, introns, UTRs, etc.  Note that model classes other than Gene can't be referred to by
     * name as they may not be in all models.
     * @param model the data model
     * @param gene the parent gene
     * @param transcript the transcript to construct a gene model for
     */
    public GeneModel(Model model, Gene gene, InterMineObject transcript) {
        this.gene = gene;
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

    /**
     * The parent gene for this gene model.
     * @return the gene
     */
    public Gene getGene() {
        return gene;
    }

    /**
     * Get the transcript this gene model represents
     * @return the transcript
     */
    public InterMineObject getTranscript() {
        return transcript;
    }

    /**
     * Get the exons if present.
     * @return the exons
     */
    public List<InterMineObject> getExons() {
        return exons;
    }

    /**
     * Get the introns if present.
     * @return the introns
     */
    public List<InterMineObject> getIntrons() {
        return introns;
    }

    /**
     * Get the CDSs if present.
     * @return the CDSs
     */
    public List<InterMineObject> getCDSs() {
        return cdss;
    }

    /**
     * Get the threePrimeUTR if present.
     * @return the threePrimeUTR
     */
    public InterMineObject getThreePrimeUTR() {
        return threePrimeUTR;
    }

    /**
     * Get the fivePrimeUTR if present.
     * @return the fivePrimeUTR
     */
    public InterMineObject getFivePrimeUTR() {
        return fivePrimeUTR;
    }

    /**
     * Return the ids of all objects represented in this gene model including the gene itself.
     * @return the ids of all objects represented in this gene model
     */
    public Set<Integer> getIds() {
        if (ids == null) {
            ids = new HashSet<Integer>();
            ids.add(gene.getId());
            ids.add(transcript.getId());
            addCollectionIds(ids, exons);
            addCollectionIds(ids, introns);
            addCollectionIds(ids, cdss);
            if (threePrimeUTR != null) {
                ids.add(threePrimeUTR.getId());
            }
            if (fivePrimeUTR != null) {
                ids.add(fivePrimeUTR.getId());
            }
        }
        return ids;
    }

    private void addCollectionIds(Set<Integer> ids, Collection<InterMineObject> col) {
        if (col != null) {
            for (InterMineObject obj : col) {
                ids.add(obj.getId());
            }
        }
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
