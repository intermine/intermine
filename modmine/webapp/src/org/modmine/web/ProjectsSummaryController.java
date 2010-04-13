package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.MetadataCache.GBrowseTrack;


public class ProjectsSummaryController extends TilesAction 
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        try {
            final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
            ObjectStore os = im.getObjectStore();
            

            Map<String, List<DisplayExperiment>> experiments = MetadataCache.getProjectExperiments(os);
            request.setAttribute("experiments", experiments);

            
            final Map<String, String> expCat = new HashMap<String,String>();
            populate(expCat);

            
            Map <String, List<DisplayExperiment>> catExp = new HashMap<String, List<DisplayExperiment>>();
            
            
            for (List<DisplayExperiment> ll : experiments.values()) {
                for (DisplayExperiment de : ll){
                    String cat = expCat.get(de.getName());
                    List<DisplayExperiment> des = catExp.get(cat);
                    if (des == null) {
                        des = new ArrayList<DisplayExperiment>();
                        catExp.put(cat, des);
                    }
                    des.add(de);
                }
            }
            

            request.setAttribute("catExp", catExp);
                              

            Map<String, List<GBrowseTrack>> tracks = MetadataCache.getExperimentGBrowseTracks(os);
            request.setAttribute("tracks", tracks);

        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

    /**
     * @param expCat
     */
    private void populate(final Map<String, String> expCat) {

        expCat.put("ChIP-Seq Identification of C. elegans TF Binding Sites", "Transcription Factor Binding Sites");
        expCat.put("Chromatin Binding Site Mapping", "Transcription Factor Binding Sites");
        expCat.put("RNA-seq support of the ChIP data", "Transcription Factor Binding Sites");

        
        expCat.put("Chromatin ChIP-chip", "Chromatin Marks and Chromatin Binding Proteins");
        expCat.put("Genomic Distributions of Histone Modifications", "Chromatin Marks and Chromatin Binding Proteins");
        expCat.put("Chromatin Binding Site Mapping", "Chromatin Marks and Chromatin Binding Proteins");
        expCat.put("RNA-seq support of the ChIP data", "Chromatin Marks and Chromatin Binding Proteins");

        
        expCat.put("Nucleosome mapping", "Chromatin Structure");
        expCat.put("Genome-wide Chromatin Profiling", "Chromatin Structure");

        
        expCat.put("Replication Timing Characterization", "DNA Replication");
        expCat.put("MacAlpine Early Origin of Replication Identification", "DNA Replication");
        expCat.put("Differential Replication of Polytene Chromosomes", "DNA Replication");
        expCat.put("Genome-wide localization of essential replication initiators", "DNA Replication");
        expCat.put("CGH drosophila cell lines", "DNA Replication");
        expCat.put("CNV in Drosophila cell lines", "DNA Replication");
        

        expCat.put("Short read sequencing of fly mRNA", "mRNA Profiling");
        expCat.put("Paired End RNA-Seq of Drosophila Cell Lines", "mRNA Profiling");
        expCat.put("Developmental Time Course Transcriptional Profiling of D. melanogaster Embryo Using SOLiD Stranded Total RNA-Seq", "mRNA Profiling");
        expCat.put("Transcriptional Profiling of additional Drosophila species with RNA-Seq", "mRNA Profiling");
        expCat.put("C-tailed RNA-Seq of Drosophila Cell lines and stages", "mRNA Profiling");
        expCat.put("Developmental Stage Timecourse Transcriptional Profiling with RNA-Seq", "mRNA Profiling");
        expCat.put("Confirmation of Drosophila transcripts by full-length cDNA screening", "mRNA Profiling");
        expCat.put("Identification of transcribed sequences with expression profile maps", "mRNA Profiling");
        expCat.put("RNA made in Bloomington", "mRNA Profiling");
        expCat.put("RNAi of RNA splicing factors in D. melanogaster", "mRNA Profiling");

        expCat.put("Definition of comprehensive set of C. elegans transcripts and expression for various stages and conditions", "mRNA Profiling");
        expCat.put("Identification of transcribed sequences under pathogenic bacterial growth conditions with expression profile maps", "mRNA Profiling");
        expCat.put("Identification of tissue and stage-specific transcribed sequences with expression profile maps", "mRNA Profiling");

        
        expCat.put("Small RNA identification", "Small RNAs Profiling");

        expCat.put("Small RNA expression in C. elegans embryos", "Small RNAs Profiling");
        expCat.put("Identification of small RNAs in C. elegans", "Small RNAs Profiling");
        expCat.put("Changes in expression of small RNAs during aging in C. elegans", "Small RNAs Profiling");        
        
        
        expCat.put("Annotation of Drosophila splice junctions by RNA-seq", "Gene Structure");
        expCat.put("Cap Analysis of Gene Expression", "Gene Structure");
        expCat.put("RT-PCR","Gene Structure");
        expCat.put("Gene Model Prediction","Gene Structure");
        expCat.put("5' RACE","Gene Structure");

        expCat.put("Encyclopedia of C. elegans 3'UTRs and their regulatory elements", "Gene Structure");
        expCat.put("Intron Confirmation in C. elegans", "Gene Structure");
        expCat.put("Definition of comprehensive set of C. elegans transcripts and expression for various stages and conditions", "Gene Structure");
        
    }
}
