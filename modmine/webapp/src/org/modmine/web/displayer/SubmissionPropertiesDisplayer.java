package org.modmine.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.Antibody;
import org.intermine.model.bio.Array;
import org.intermine.model.bio.CellLine;
import org.intermine.model.bio.DevelopmentalStage;
import org.intermine.model.bio.Strain;
import org.intermine.model.bio.Submission;
import org.intermine.model.bio.SubmissionProperty;
import org.intermine.model.bio.Tissue;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.json.JSONArray;

/**
 * Join submission properties to one table.
 * Refer to OverlappingFeaturesDisplayer and RegulatoryRegionsDisplayer.
 *
 * @author Fengyuan Hu
 *
 */
public class SubmissionPropertiesDisplayer extends ReportDisplayer
{
//    private static String submissionPropertyJSON = null;

    protected static final Logger LOG = Logger.getLogger(SubmissionPropertiesDisplayer.class);

    /**
     * @param config ReportDisplayerConfig
     * @param im InterMineAPI
     */
    public SubmissionPropertiesDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        Submission sub = (Submission) reportObject.getObject();

        //== Organism ==
        Map<Integer, String> organismMap = new HashMap<Integer, String>();
        organismMap.put(sub.getOrganism().getId(), sub.getOrganism().getShortName());
        request.setAttribute("organismMap", organismMap);

        //== Antibody ==
        Set<Antibody> antibodies = new HashSet<Antibody>();
        for (Antibody a : sub.getAntibodies()) {
            if (!"not applicable".equals(a.getName())) {
                antibodies.add(a);
            }
        }
        request.setAttribute("antibodyInfoList", antibodies);

        //== CellLine ==
        Map<Integer, String> cellLineMap = new HashMap<Integer, String>();

        for (CellLine c : sub.getCellLines()) {
            if (!"not applicable".equals(c.getName())) {
                cellLineMap.put(c.getId(), c.getName());
            }
        }

        request.setAttribute("cellLineMap", cellLineMap);

        //== DevelopmentalStage ==
        Map<Integer, String> developmentalStageMap = new HashMap<Integer, String>();

        for (DevelopmentalStage d : sub.getDevelopmentalStages()) {
            if (!"not applicable".equals(d.getName())) {
                developmentalStageMap.put(d.getId(), d.getName());
            }
        }

        request.setAttribute("developmentalStageMap", developmentalStageMap);

        //== Strain ==

        Set<Strain> strains = new HashSet<Strain>();
        for (Strain s : sub.getStrains()) {
            if (!"not applicable".equals(s.getName())) {
                strains.add(s);
            }
        }
        request.setAttribute("strainInfoList", strains);

//        Map<Integer, String> strainMap = new HashMap<Integer, String>();
//        for (Strain s : sub.getStrains()) {
//            if (!"not applicable".equals(s.getName())) {
//                strainMap.put(s.getId(), s.getName());
//            }
//        }
//        request.setAttribute("strainMap", strainMap);

        //== Tissue ==
        Map<Integer, String> tissueMap = new HashMap<Integer, String>();

        for (Tissue t : sub.getTissues()) {
            if (!"not applicable".equals(t.getName())) {
                tissueMap.put(t.getId(), t.getName());
            }
        }

        request.setAttribute("tissueMap", tissueMap);

        //== Array ==
        Map<Integer, String> arrayMap = new HashMap<Integer, String>();

        for (Array a : sub.getArrays()) {
            if (!"not applicable".equals(a.getName())) {
                arrayMap.put(a.getId(), a.getName());
            }
        }

        request.setAttribute("arrayMap", arrayMap);

        // set the technique for the properties displayer
        request.setAttribute("technique", sub.getExperimentType());


        //== SubmissionProperty ==
        request.setAttribute("submissionPropertyJSON", getSubmissionPropertyJSON(sub));
    }

    private static synchronized String getSubmissionPropertyJSON(Submission sub) {

        String submissionPropertyJSON = null;

        Map<String, Map<Integer, String>> submissionPropertyMap =
            new HashMap<String, Map<Integer, String>>();

        Set<SubmissionProperty> spSet = sub.getProperties();

        if (spSet == null || spSet.size() < 1) {
            return null;
        } else {
            for (SubmissionProperty sp : sub.getProperties()) {
                if ("SubmissionPropertyShadow".equals(sp.getClass().getSimpleName())) {
                    if (!submissionPropertyMap.containsKey(sp.getType())) {
                        Map<Integer, String> propertyMap = new HashMap<Integer, String>();
                        propertyMap.put(sp.getId(), sp.getName());
                        submissionPropertyMap.put(sp.getType(), propertyMap);
                    } else {
                        submissionPropertyMap.get(sp.getType()).put(sp.getId(), sp.getName());
                    }
                }
            }

            // Parse map to json
            List<Object> propertiesList = new ArrayList<Object>();

            for (Entry<String, Map<Integer, String>> e : submissionPropertyMap.entrySet()) {
                Map<String, Object> propertiesMap = new LinkedHashMap<String, Object>();
                propertiesMap.put("type", e.getKey());
                List<Object> valueList = new ArrayList<Object>();
                for (Entry<Integer, String> en : ((Map<Integer, String>) e
                        .getValue()).entrySet()) {
                    Map<String, Object> valueMap = new LinkedHashMap<String, Object>();
                    valueMap.put("id", en.getKey());
                    valueMap.put("name", en.getValue());
                    valueList.add(valueMap);
                }
                propertiesMap.put("value", valueList);
                propertiesList.add(propertiesMap);
            }

            JSONArray ja = new JSONArray(propertiesList);
            submissionPropertyJSON = ja.toString();

            return submissionPropertyJSON;
        }
    }
}