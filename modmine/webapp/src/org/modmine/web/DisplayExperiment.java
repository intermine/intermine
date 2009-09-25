package org.modmine.web;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.model.bio.Experiment;
import org.intermine.model.bio.ExperimentalFactor;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Project;
import org.intermine.model.bio.Submission;


/**
 * Wrap an experiment and its submissions to make display code simpler.
 * @author Richard Smith
 *
 */
public class DisplayExperiment {

    private String name;
    private List<Submission> submissions = new ArrayList<Submission>();
    private String projectName;
    private String pi;
    private String description = null;
    private Set<String> factorTypes = new HashSet<String>();
    private Set<String> organisms = new HashSet<String>();
    
    
    /**
     * Construct with objects from database. 
     * @param exp the experiment
     * @param project the experiment's project
     */
    public DisplayExperiment(Experiment exp, Project project) {
        initialise(exp, project);
    }
    
    
    private void initialise(Experiment exp, Project proj) {
        this.name = exp.getName();
        if (name.indexOf('&') > 0) {
            name = name.substring(0, name.indexOf('&'));
        }
        
        this.pi = proj.getNamePI() + " " + proj.getSurnamePI();
        this.projectName = proj.getName();

        for (Submission submission : exp.getSubmissions()) {
            if (this.description == null) {
                this.description = submission.getDescription();
            }
            submissions.add(submission);
            for (ExperimentalFactor factor : submission.getExperimentalFactors()) {
                factorTypes.add(factor.getType());
            }
        }
        
        for (Organism organism : proj.getOrganisms()) {
            organisms.add(organism.getTaxonId().toString());
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
    
}
