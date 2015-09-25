package org.flymine.web.displayer;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.map.ListOrderedMap;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ResultElement;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Homologue;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Displayer for drosophila homologue data
 * @author rns
 */
public class DrosophilaHomologueDisplayer extends ReportDisplayer
{

    private static final List<String> SPECIES = Arrays.asList(new String[] {"grimshawi",
        "virilis", "mojavensis", "willistoni", "persimilis", "pseudoobscura", "ananassae",
        "erecta", "yakuba", "melanogaster", "sechellia", "simulans"});
    private static final String HOMOLOGY_DATASET = "Drosophila 12 Genomes Consortium homology";
    private static final String GENUS = "Drosophila";
    /**
     * @param config report object config
     * @param im intermine API
     */
    public DrosophilaHomologueDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        Map<String, Set<ResultElement>> homologues = initMap();
        Map<String, String> organismIds = new HashMap<String, String>();

        Path symbolPath = null;
        try {
            symbolPath = new Path(im.getModel(), "Gene.symbol");
        } catch (PathException e) {
            return;
        }

        Gene gene = (Gene) reportObject.getObject();
        boolean isRecentred = !GENUS.equals(gene.getOrganism().getGenus());
        String thisSpecies = gene.getOrganism().getGenus();
        // show displayer on Drosophila report pages only
        if (GENUS.equals(thisSpecies)) {
            request.setAttribute("willBeDisplayed", Boolean.TRUE);

            if (isRecentred) {
                request.setAttribute("origSymbol", gene.getSymbol());
            HOMOLOGUES:
                for (Homologue homologue : gene.getHomologues()) {
                    for (DataSet dataSet : homologue.getDataSets()) {
                        if (HOMOLOGY_DATASET.equals(dataSet.getName())) {
                            String species = homologue.getHomologue().getOrganism().getSpecies();
                            if ("melanogaster".equals(species)) {
                                ResultElement re = new ResultElement(homologue.getHomologue(),
                                           symbolPath, true);
                                addToMap(homologues, species, re);
                                gene = homologue.getHomologue();
                                break HOMOLOGUES;
                            }
                        }
                    }
                }
            }

            for (Homologue homologue : gene.getHomologues()) {
                for (DataSet dataSet : homologue.getDataSets()) {
                    if (HOMOLOGY_DATASET.equals(dataSet.getName())) {
                        Organism org = homologue.getHomologue().getOrganism();
                        organismIds.put(org.getSpecies(), org.getId().toString());
                        ResultElement re = new ResultElement(homologue.getHomologue(),
                                symbolPath, true);
                        String species = org.getSpecies();
                        if (!isRecentred || (isRecentred && !thisSpecies.equals(species))) {
                            addToMap(homologues, species, re);
                        }
                    }
                }
            }

            request.setAttribute("organismIds", organismIds);
            request.setAttribute("isRecentred", isRecentred);
            request.setAttribute("homologues", homologues);
        }
    }

    private Map<String, Set<ResultElement>> initMap() {
        Map<String, Set<ResultElement>> homologues = new ListOrderedMap();
        for (String species : SPECIES) {
            addToMap(homologues, species, null);
        }
        return homologues;
    }

    private void addToMap(Map<String, Set<ResultElement>> homologues, String species,
            ResultElement re) {
        Set<ResultElement> speciesHomologues = homologues.get(species);
        if (speciesHomologues == null) {
            speciesHomologues = new HashSet<ResultElement>();
            homologues.put(species, speciesHomologues);
        }
        if (re != null) {
            speciesHomologues.add(re);
        }
    }
}
