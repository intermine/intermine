package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.web.logic.Constants;

/**
 * @author Matthew Wakeling
 * @author Xavier Watkins
 */
public class DisplayLookupMessageHandler
{
    private static Set<String> unresolved, duplicates, translated, lowQuality;
    @SuppressWarnings("rawtypes")
    private static Map<String, List> wildcards;

    /**
     * Generates and saves messages for bag lookup
     *
     * @param bqr the bag query results
     * @param session the session
     * @param properties properties
     * @param type the type
     * @param extraConstraint the extra constraint
     */
    public static void handleMessages(BagQueryResult bqr, HttpSession session,
                                      Properties properties, String type, String extraConstraint) {
        fillInArrays(bqr);
        ActionMessages actionMessages = new ActionMessages();
        if (unresolved.size() > 0) {
            StringBuffer values = new StringBuffer();
            for (String value : unresolved) {
                if (values.length() > 0) {
                    values.append(", ");
                }
                values.append(value);
            }
            if (unresolved.size() == 1) {
                ActionMessage msg = new ActionMessage("results.lookup.unresolved.one", type,
                                                      values.toString());
                actionMessages.add(Constants.LOOKUP_MSG, msg);
            } else
                if (unresolved.size() >= 1) {
                    ActionMessage msg = new ActionMessage("results.lookup.unresolved.many", type,
                                                          values.toString());
                    actionMessages.add(Constants.LOOKUP_MSG, msg);
                }
            if (extraConstraint != null && extraConstraint.length() != 0) {
                ActionMessage msg = new ActionMessage("results.lookup.in", extraConstraint);
                actionMessages.add(Constants.LOOKUP_MSG, msg);
            }
        }
        if (duplicates.size() > 0) {
            StringBuffer values = new StringBuffer();
            for (String value : duplicates) {
                if (values.length() > 0) {
                    values.append(", ");
                }
                values.append(value);
            }
// temporarily remove confusing message of dubious value.  See #2270.
//            if (duplicates.size() == 1) {
//                ActionMessage msg = new ActionMessage("results.lookup.duplicate.one",
//                                                      values.toString());
//                actionMessages.add(Constants.LOOKUP_MSG, msg);
//            } else {
//                ActionMessage msg = new ActionMessage("results.lookup.duplicate.many",
//                                                      values.toString());
//                actionMessages.add(Constants.LOOKUP_MSG, msg);
//            }
        }
        if (translated.size() > 0) {
            StringBuffer values = new StringBuffer();
            for (String value : translated) {
                if (values.length() > 0) {
                    values.append(", ");
                }
                values.append(value);
            }
            if (translated.size() == 1) {
                ActionMessage msg = new ActionMessage("results.lookup.translated.one", type,
                                                      values.toString());
                actionMessages.add(Constants.LOOKUP_MSG, msg);
            } else {
                ActionMessage msg = new ActionMessage("results.lookup.translated.many", type,
                                                      values.toString());
                actionMessages.add(Constants.LOOKUP_MSG, msg);
            }
        }
        if (lowQuality.size() > 0) {
            StringBuffer values = new StringBuffer();
            for (String value : lowQuality) {
                if (values.length() > 0) {
                    values.append(", ");
                }
                values.append(value);
            }
            if (lowQuality.size() == 1) {
                ActionMessage msg = new ActionMessage("results.lookup.lowQuality.one",
                                                      values.toString());
                actionMessages.add(Constants.LOOKUP_MSG, msg);
            } else {
                ActionMessage msg = new ActionMessage("results.lookup.lowQuality.many",
                                                      values.toString());
                actionMessages.add(Constants.LOOKUP_MSG, msg);
            }
        }
        if (wildcards.size() > 0) {
            if (wildcards.size() == 1) {
                @SuppressWarnings("unchecked")
                List<String> list = wildcards.values().iterator().next();
                String key = wildcards.keySet().iterator().next();
                if (list.size() == 1) {
                    ActionMessage msg = new ActionMessage("results.lookup.wildcard.oneone", key);
                    actionMessages.add(Constants.LOOKUP_MSG, msg);
                } else {
                    ActionMessage msg = new ActionMessage("results.lookup.wildcard.one",
                            key + " (" + wildcards.get(key).size() + ")");
                    actionMessages.add(Constants.LOOKUP_MSG, msg);
                }
            } else {
                StringBuffer values = new StringBuffer();
                for (String value : wildcards.keySet()) {
                    if (values.length() > 0) {
                        values.append(", ");
                    }
                    values.append(value);
                }
                ActionMessage msg = new ActionMessage("results.lookup.wildcard.many", values);
                actionMessages.add(Constants.LOOKUP_MSG, msg);
            }
        }
        session.setAttribute(Constants.LOOKUP_MSG, actionMessages);
    }

    /**
     * Fills in arrays from the BagQueryResult
     * @param bqr the bag query Result object
     */
    @SuppressWarnings("rawtypes")
    private static void fillInArrays(BagQueryResult bqr) {
        unresolved = new HashSet<String>(bqr.getUnresolvedIdentifiers());
        duplicates = new HashSet<String>();
        lowQuality = new HashSet<String>();
        translated = new HashSet<String>();
        wildcards = new HashMap<String, List>();
        Map<String, Map<String, List>> duplicateMap = bqr.getIssues().get(BagQueryResult.DUPLICATE);
        if (duplicateMap != null) {
            for (Map.Entry<String, Map<String, List>> queries : duplicateMap.entrySet()) {
                duplicates.addAll(queries.getValue().keySet());
            }
        }
        Map<String, Map<String, List>> translatedMap = bqr.getIssues().get(
            BagQueryResult.TYPE_CONVERTED);
        if (translatedMap != null) {
            for (Map.Entry<String, Map<String, List>> queries : translatedMap.entrySet()) {
                translated.addAll(queries.getValue().keySet());
            }
        }
        Map<String, Map<String, List>> lowQualityMap = bqr.getIssues().get(BagQueryResult.OTHER);
        if (lowQualityMap != null) {
            for (Map.Entry<String, Map<String, List>> queries : lowQualityMap.entrySet()) {
                lowQuality.addAll(queries.getValue().keySet());
            }
        }
        Map<String, Map<String, List>> wildcardMap = bqr.getIssues().get(BagQueryResult.WILDCARD);
        if (wildcardMap != null) {
            for (Map.Entry<String, Map<String, List>> queries : wildcardMap.entrySet()) {
                wildcards.putAll(queries.getValue());
            }
        }
    }

    /**
     * Check if there are any identifiers that didn't match.
     *
     * @return true if there are any issues
     */
    public boolean isIssues() {
        return (!unresolved.isEmpty() || !duplicates.isEmpty() || !translated.isEmpty()
                || !wildcards.isEmpty() || !lowQuality.isEmpty());
    }

}
