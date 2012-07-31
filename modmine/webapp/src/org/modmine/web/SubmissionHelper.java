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
import java.util.List;

import org.intermine.model.bio.ExperimentalFactor;
import org.intermine.model.bio.Submission;
import org.intermine.util.StringUtil;

/**
 * Helper methods for dealing with submissions.
 * @author Richard Smith
 *
 */
public class SubmissionHelper
{
    
    /**
     * Get a display string of the experimental factors for the given submission.
     * @param sub the submission
     * @return a display string of experimental factors
     */
    public static String getExperimentalFactorString(Submission sub) {
        List<String> factors = new ArrayList<String>();
        for (ExperimentalFactor factor : sub.getExperimentalFactors()) {
            factors.add(factor.getType() + " " + factor.getName());
        }   
        return StringUtil.prettyList(factors);
    }
}
