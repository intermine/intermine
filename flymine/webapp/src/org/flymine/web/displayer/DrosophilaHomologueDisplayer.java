package org.flymine.web.displayer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.map.ListOrderedMap;
import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ResultElement;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Homologue;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.DisplayObject;

public class DrosophilaHomologueDisplayer extends CustomDisplayer {

    private static final List<String> SPECIES = Arrays.asList(new String[] {"grimshawi", "virilis",
            "mojavensis", "willistoni", "persimilis", "pseudoobscura", "ananassae", "erecta",
            "yakuba", "melanogaster", "sechellia", "simulans"});
    private static final String HOMOLOGY_DATASET = "Drosophila 12 Genomes Consortium homology";

    public DrosophilaHomologueDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, DisplayObject displayObject) {

        Map<String, Set<ResultElement>> homologues = initMap();

        Path symbolPath = null;
        try {
            symbolPath = new Path(im.getModel(), "Gene.symbol");
        } catch (PathException e) {
            return;
        }

        Gene gene = (Gene) displayObject.getObject();
        for (Homologue homologue : gene.getHomologues()) {
            for (DataSet dataSet : homologue.getDataSets()) {
                if (HOMOLOGY_DATASET.equals(dataSet.getName())) {
                    ResultElement re = new ResultElement(homologue.getHomologue(),
                            symbolPath, true);
                    String species = homologue.getHomologue().getOrganism().getSpecies();
                    addToMap(homologues, species, re);
                }
            }
        }
        request.setAttribute("homologues", homologues);
    }

    private Map<String, Set<ResultElement>> initMap() {
        Map homologues = new ListOrderedMap();
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
