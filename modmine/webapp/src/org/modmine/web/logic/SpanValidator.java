package org.modmine.web.logic;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.bio.web.model.GenomicRegion;
import org.modmine.web.ChromosomeInfo;

/**
 * This Class validates if the user input has errors.
 *
 * @author Fengyuan Hu
 *
 */
public class SpanValidator
{
    /**
     *
     * @param orgName organism name
     * @param spanList A list of spans
     * @param chrInfoMap a java bean
     * @return resultMap A HashMap
     */
    public Map<String, List<GenomicRegion>> runSpanValidation(String orgName,
            List<GenomicRegion> spanList,
            Map<String, List<ChromosomeInfo>> chrInfoMap) {

        // the Map has two key-value mappings
        // PASS-ArrayList<passedSpan>
        // ERROR-ArrayList<errorSpan>
        Map<String, List<GenomicRegion>> resultMap = new HashMap<String, List<GenomicRegion>>();
        List<GenomicRegion> passedSpanList = new ArrayList<GenomicRegion>();
        List<GenomicRegion> errorSpanList = new ArrayList<GenomicRegion>();

        List<ChromosomeInfo> chrInfoList = chrInfoMap.get(orgName);

        // make passedSpanList
        for (GenomicRegion aSpan : spanList) {
            for (ChromosomeInfo chrInfo : chrInfoList) {
                if (aSpan.getChr().equals(chrInfo.getChrPID())) {
                    if ((aSpan.getStart() >= 0 && aSpan.getStart() <= chrInfo
                            .getChrLength())
                            && (aSpan.getEnd() >= 0 && aSpan.getEnd() <= chrInfo
                                    .getChrLength())) {
                        if (aSpan.getStart() > aSpan.getEnd()) { // Start must be smaller than End
                            GenomicRegion newSpan = new GenomicRegion();
                            newSpan.setChr(aSpan.getChr());
                            newSpan.setStart(aSpan.getEnd());
                            newSpan.setEnd(aSpan.getStart());
                            passedSpanList.add(newSpan);
                        } else {
                            passedSpanList.add(aSpan);
                        }
                    }
                }
            }

        }

        // make errorSpanList
        for (GenomicRegion aSpan : spanList) {
            if (!passedSpanList.contains(aSpan)) {
                errorSpanList.add(aSpan);
            }
        }

        resultMap.put("pass", passedSpanList);
        resultMap.put("error", errorSpanList);

        return resultMap;
    }

}
