package org.modmine.web;

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
import java.util.List;
import java.util.Map;

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
    public static Map<String, List<Span>> runSpanValidation(String orgName, List<Span> spanList,
            Map<String, List<ChromosomeInfo>> chrInfoMap) {

        // the Map has two key-value mappings
        // PASS-ArrayList<passedSpan>
        // ERROR-ArrayList<errorSpan>
        Map<String, List<Span>> resultMap = new HashMap<String, List<Span>>();
        List<Span> passedSpanList = new ArrayList<Span>();
        List<Span> errorSpanList = new ArrayList<Span>();

        List<ChromosomeInfo> chrInfoList = chrInfoMap.get(orgName);

        // make passedSpanList
        for (Span aSpan : spanList) {
            for (ChromosomeInfo chrInfo : chrInfoList) {
                if (aSpan.getChr().equals(chrInfo.getChrPID())) {
                    if ((aSpan.getStart() >= 0 && aSpan.getStart() <= chrInfo
                            .getChrLength())
                            && (aSpan.getEnd() >= 0 && aSpan.getEnd() <= chrInfo
                                    .getChrLength())) {
                        if (aSpan.getStart() > aSpan.getEnd()) { // Start must be smaller than End
                            Span newSpan = new Span();
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
        for (Span aSpan : spanList) {
            if (!passedSpanList.contains(aSpan)) {
                errorSpanList.add(aSpan);
            }
        }

        resultMap.put("pass", passedSpanList);
        resultMap.put("error", errorSpanList);

        return resultMap;
    }

}
