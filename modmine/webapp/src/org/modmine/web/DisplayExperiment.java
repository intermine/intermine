package org.modmine.web;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.intermine.model.bio.Experiment;
import org.intermine.model.bio.ExperimentalFactor;
import org.intermine.model.bio.Lab;
import org.intermine.model.bio.Project;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.StringUtil;


/**
 * Wrap an experiment and its submissions to make display code simpler.
 * @author Richard Smith
 *
 */
public class DisplayExperiment
{

    private String name;
    private List<Submission> submissions = new ArrayList<Submission>();
    private String projectName;
    private String pi;
    private String description = null;
    private Set<String> factorTypes = new TreeSet<String>(new FactorTypeComparator());
    private Set<String> organisms = new HashSet<String>();
    private Set<FeatureCountsRecord> featureCountsRecords =
        new LinkedHashSet<FeatureCountsRecord>();
    private ObjectStore os;
    private String experimentType;
    private String experimentCategory;
    private Set<String> labs = new TreeSet<String>();
    private String piSurname;

    /**
     * Construct with objects from database and feature counts summary map.
     * @param exp the experiment
     * @param project the experiment's project
     * @param featureCountsRecords a set of FeatureCountsRecord
     * @param os the objectstore
     */
    public DisplayExperiment(Experiment exp, Project project,
            Set<FeatureCountsRecord> featureCountsRecords, ObjectStore os) {
        initialise(exp, project);
        this.featureCountsRecords = featureCountsRecords;
        this.os = os;
    }

    private void initialise(Experiment exp, Project proj) {
        this.name = exp.getName();
        if (name.indexOf('&') > 0) {
            name = name.substring(0, name.indexOf('&'));
        }

        this.pi = (proj.getNamePI() == null ? "" : proj.getNamePI() + " ") + proj.getSurnamePI();
        this.piSurname = proj.getSurnamePI();
        if (!StringUtils.isBlank(proj.getName())) {
            this.projectName = proj.getName();
        }

        Set<String> expTypes = new HashSet<String>();

        for (Submission submission : exp.getSubmissions()) {
            if (this.description == null) {
                this.description = submission.getDescription();
            }
            submissions.add(submission);
            Lab lab = submission.getLab();
            if (lab.getName() != null) {
                labs.add(submission.getLab().getName());
            } else {
                labs.add(lab.getSurname());
            }
            organisms.add(submission.getOrganism().getShortName());
            for (ExperimentalFactor factor : submission.getExperimentalFactors()) {
                factorTypes.add(factor.getType());
            }

            if (submission.getExperimentType() != null) {
                expTypes.add(submission.getExperimentType());
            }
        }

        this.experimentType = StringUtil.prettyList(expTypes);

        this.experimentCategory = exp.getCategory();

//        for (Organism organism : proj.getOrganisms()) {
//            organisms.add(organism.getShortName());
//        }
    }

    private class FactorTypeComparator implements Comparator<String>
    {
        final String DEVELOPMENTALSTAGE = "developmental stage";
        final String ANTIBODYTARGET = "antibody target";
        final String ANTIBODY = "antibody";

        public int compare(String ft1, String ft2) {

            if (ft1.equals(ANTIBODYTARGET) &&  ft2.equals(ANTIBODYTARGET)) {
                return 0;
            } else if (ft1.equals(ANTIBODYTARGET) || ft2.equals(ANTIBODYTARGET)) {
                if (ft1.equals(ANTIBODYTARGET)) {
                    return -1;
                } else {
                    return 1;
                }
            }
            if (ft1.equals(DEVELOPMENTALSTAGE) && ft2.equals(DEVELOPMENTALSTAGE)) {
                return 0;
            } else if (ft1.equals(DEVELOPMENTALSTAGE) || ft2.equals(DEVELOPMENTALSTAGE)) {
                if (ft1.equals(DEVELOPMENTALSTAGE)) {
                    return -1;
                } else {
                    return 1;
                }
            }

            if (ft1.equals(ANTIBODY) && ft2.equals(ANTIBODY)) {
                return 0;
            } else if (ft1.equals(ANTIBODY) || ft2.equals(ANTIBODY)) {
                if (ft1.equals(ANTIBODY)) {
                    return -1;
                } else {
                    return 1;
                }
            }

            return ft1.compareTo(ft2);
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }


    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }


    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @return the piSurname
     */
    public String getPiSurname() {
        return piSurname;
    }


    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the submissions
     */
    public List<Submission> getSubmissions() {
        return submissions;
    }

    /**
     * @return the submissions Ids
     */
    public List<String> getSubmissionsDccId() {
        List<String> subDccIds = new ArrayList<String>();
        for (Submission sub : submissions) {
            subDccIds.add(sub.getdCCid().toString());
        }
        return subDccIds;
    }

    /**
     * @return submissions and a map of feature type to count
     */
    public Map<Submission, Map<String, Long>> getSubmissionsAndFeatureCounts() {
        Map<Submission, Map<String, Long>> subMap = new HashMap<Submission, Map<String, Long>>();
        for (Submission sub : submissions) {
            subMap.put(sub, MetadataCache.getSubmissionFeatureCounts(os, sub.getdCCid()));
        }
        return subMap;
    }

    /**
     * @return the factorTypes
     */
    public Set<String> getFactorTypes() {
        return factorTypes;
    }


    /**
     * @return the organisms
     */
    public Set<String> getOrganisms() {
        return organisms;
    }

    /**
     * @return the count of submissions
     */
    public int getSubmissionCount() {
        return submissions.size();
    }

    /**
     * @return the featureCountsRecords
     */
    public Set<FeatureCountsRecord> getFeatureCountsRecords() {
        return featureCountsRecords;
    }

    /**
     * @return the number of experimental factors
     */
    public int getFactorCount() {
        return factorTypes.size();
    }

    /**
     * @return the number of entries submitted to a public repository
     * for this experiment
     */
    public int getRepositedCount() {
        Integer count = MetadataCache.getExperimentRepositoryCount(os).get(name);
        return count;
    }

    /**
     * @return the number of expression levels for a submission
     * for this experiment
     */
    public int getExpressionLevelCount() {
        Integer count = MetadataCache.getExperimentExpressionLevels(os).get(name);
        return count;
    }

    /**
     * @return a map of entries per db submitted to a public repository
     * for this experiment
     */
    public Map<String, Integer> getReposited() {
        Set<String[]> rep = MetadataCache.getExperimentRepositoryEntries(os).get(name);
        Map<String, Integer> dbMap = new HashMap<String, Integer>();

        for (String[] s : rep) {
            addToCounterMap(dbMap, s[0]);
        }
        return dbMap;
    }

     /**
     * @return a map of unlocated features for this experiment
     */
    public Set<String> getUnlocated() {
        Map<String, List<String>> rep = MetadataCache.getUnlocatedFeatureTypes(os);

        Set<String> unloc = new HashSet<String>();
        for (Submission s : submissions) {
            if (rep.get(s.getdCCid()) != null) {
                unloc.addAll(rep.get(s.getdCCid()));
            }
        }
        return unloc;
    }

    /**
     * @return a map of features with sequence for this experiment
     */
    public Set<String> getSequenced() {
        Map<String, List<String>> seq = MetadataCache.getSequencedFeatureTypes(os);

        Set<String> sequenced = new HashSet<String>();
        for (Submission s : submissions) {
            if (seq.get(s.getdCCid()) != null) {
                sequenced.addAll(seq.get(s.getdCCid()));
            }
        }
        return sequenced;
    }

    /**
     * adds the elements of a list i to a list l only if they are not yet
     * there
     * @param l the receiving list
     * @param i the donating list
     */
    private static void addToCounterMap(Map<String, Integer> m, String s) {

        Integer counter = 1;
        if (m.containsKey(s)) {
            counter = m.get(s) + 1;
        }
        m.put(s, counter);
    }

    /**
     * @return the experimentType
     */
    public String getExperimentType() {
        return experimentType;
    }

    /**
     * @return the experimentType
     */
    public String getCategory() {
        return experimentCategory;
    }

    /**
     * @return the labs
     */
    public Set<String> getLabs() {
        return labs;
    }
}
