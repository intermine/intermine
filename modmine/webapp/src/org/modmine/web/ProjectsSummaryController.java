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
            expCat.put("Small RNA expression in C. elegans embryos", "Transcriptional profiling");
            expCat.put("Changes in expression of small RNAs during aging in C. elegans", "Transcriptional profiling");
            expCat.put("Definition of comprehensive set of C. elegans transcripts and expression for various stages and conditions", "Transcriptional profiling");
            expCat.put("Short read sequencing of fly mRNA", "Transcriptional profiling");
            expCat.put("Paired End RNA-Seq of Drosophila Cell Lines", "Transcriptional profiling");
            expCat.put("Developmental Time Course Transcriptional Profiling of D. melanogaster Embryo Using SOLiD Stranded Total RNA-Seq", "Transcriptional profiling");
            expCat.put("Transcriptional Profiling of additional Drosophila species with RNA-Seq", "Transcriptional profiling");
            expCat.put("C-tailed RNA-Seq of Drosophila Cell lines and stages", "Transcriptional profiling");
            expCat.put("Developmental Stage Timecourse Transcriptional Profiling with RNA-Seq", "Transcriptional profiling");
            expCat.put("RNAi of RNA splicing factors in D. melanogaster", "Transcriptional profiling");
            expCat.put("Identification of transcribed sequences under pathogenic bacterial growth conditions with expression profile maps", "Transcriptional profiling");
            expCat.put("Identification of tissue and stage-specific transcribed sequences with expression profile maps", "Transcriptional profiling");
            expCat.put("Confirmation of Drosophila transcripts by full-length cDNA screening", "Transcriptional profiling");
            expCat.put("Identification of transcribed sequences with expression profile", "Transcriptional profiling");
            
            expCat.put("Small RNA identification", "Small RNAs");
            expCat.put("Identification of small RNAs in C. elegans", "Small RNAs");
            
            expCat.put("Encyclopedia of C. elegans 3'UTRs and their regulatory elements", "Genome Annotation");
            expCat.put("Annotation of Drosophila splice junctions by RNA-seq", "Genome Annotation");
            expCat.put("Cap Analysis of Gene Expression", "Genome Annotation");
            expCat.put("Intron Confirmation in C. elegans", "Genome Annotation");
            expCat.put("RT-PCR","Genome Annotation");
            expCat.put("Gene Model Prediction","Genome Annotation");
            expCat.put("5' RACE","Genome Annotation");

            expCat.put("Nucleosome mapping", "Chromatin Structure");
            expCat.put("Genome-wide Chromatin Profiling", "Chromatin Structure");
            
            expCat.put("Replication Timing Characterization", "DNA replication");
            expCat.put("MacAlpine Early Origin of Replication Identification", "DNA replication");
            expCat.put("Differential Replication of Polytene Chromosomes", "DNA replication");
            expCat.put("Genome-wide localization of essential replication initiators", "DNA replication");
            expCat.put("CGH drosophila cell lines", "DNA replication");
            expCat.put("CNV in Drosophila cell lines", "DNA replication");

            
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
}
