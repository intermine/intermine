package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.HashMap;
import java.util.Map;

public class TemplatesExecutionMap {
    private Map<String, Map<String, Integer>> executionMapByUser =
                                              new HashMap<String, Map<String, Integer>>();
    private Map<String, Map<String, Integer>> privateExecutionMapByUser =
                                              new HashMap<String, Map<String, Integer>>();
    private Map<String, Map<String, Integer>> executionMapByAnonymous =
                                              new HashMap<String, Map<String, Integer>>();

    public void addExecution(TemplateTrack templateTrack) {
        if (!"".equals(templateTrack.getUserName())) {
            if (templateTrack.isTemplatePublic()) {
                addExecutionByUser(templateTrack.getTemplateName(), templateTrack.getUserName());
            } else {
                addPrivateExecutionByUser(templateTrack.getTemplateName(), templateTrack.getUserName());
            }
        }
        else {
            addExecutionBySessionIdentifier(templateTrack.getTemplateName(), templateTrack.getSessionIdentifier());
        }
    }

    public synchronized void addExecutionByUser(String template, String userName) {
        Map<String, Integer> executionByUser;
        if (!executionMapByUser.containsKey(template)) {
            executionByUser = new HashMap<String, Integer>();
            executionByUser.put(userName, 1);
            executionMapByUser.put(template, executionByUser);
        }
        else {
            executionByUser = executionMapByUser.get(template);
            if (!executionByUser.containsKey(userName)) {
                executionByUser.put(userName, 1);
            } else {
                executionByUser.put(userName, executionByUser.get(userName).intValue() + 1);
            }
        }
    }

    public synchronized void addPrivateExecutionByUser(String template, String userName) {
        Map<String, Integer> executionByUser;
        if (!privateExecutionMapByUser.containsKey(template)) {
            executionByUser = new HashMap<String, Integer>();
            executionByUser.put(userName, 1);
            privateExecutionMapByUser.put(template, executionByUser);
        }
        else {
            executionByUser = privateExecutionMapByUser.get(template);
            if (!executionByUser.containsKey(userName)) {
                executionByUser.put(userName, 1);
            } else {
                executionByUser.put(userName, executionByUser.get(userName).intValue() + 1);
            }
        }
    }

    public synchronized void addExecutionBySessionIdentifier(String template, String sessionId) {
        Map<String, Integer> executionBySessionIdentifier;
        if (!executionMapByAnonymous.containsKey(template)) {
            executionBySessionIdentifier = new HashMap<String, Integer>();
            executionBySessionIdentifier.put(sessionId, 1);
            executionMapByAnonymous.put(template, executionBySessionIdentifier);
        }
        else {
            executionBySessionIdentifier = executionMapByAnonymous.get(template);
            if (!executionBySessionIdentifier.containsKey(sessionId)) {
                executionBySessionIdentifier.put(sessionId, 1);
            } else {
                executionBySessionIdentifier.put(sessionId,
                    executionBySessionIdentifier.get(sessionId).intValue() + 1);
            }
        }
    }

    public Map<String, Double> getLnUserMap(String userName, boolean isSuperUser) {
        Map<String, Double> lnUserMap = new HashMap<String, Double>();
        if (userName == null || (userName != null && isSuperUser)) {
            for (String templateName : executionMapByUser.keySet()) {
                Map<String, Integer> execution = executionMapByUser.get(templateName);
                double accessLn = 0;
                for (String user : execution.keySet()) {
                    accessLn = accessLn + Math.log(execution.get(user) + 1);
                }
                lnUserMap.put(templateName, accessLn);
            }
        } else {
            for (String templateName : executionMapByUser.keySet()) {
                Map<String, Integer> execution = executionMapByUser.get(templateName);
                if (execution.containsKey(userName)) {
                    double accessLn = Math.log(execution.get(userName) + 1);
                    lnUserMap.put(templateName, accessLn);
                }
            }
            for (String templateName : privateExecutionMapByUser.keySet()) {
                Map<String, Integer> execution = privateExecutionMapByUser.get(templateName);
                if (execution.containsKey(userName)) {
                    double accessLn = Math.log(execution.get(userName) + 1);
                    lnUserMap.put(templateName, accessLn);
                }
            }
        }
        return lnUserMap;
    }

    public Map<String, Double> getLnAnonymousMap(String sessionId) {
        Map<String, Double> lnAnonymousMap = new HashMap<String, Double>();
        if (sessionId == null) {
            for (String templateName : executionMapByAnonymous.keySet()) {
                Map<String, Integer> execution = executionMapByAnonymous.get(templateName);
                double accessLn = 0;
                for (String session : execution.keySet()) {
                    accessLn = accessLn + Math.log(execution.get(session) + 1);
                }
                lnAnonymousMap.put(templateName, accessLn);
            }
        } else {
            for (String templateName : executionMapByAnonymous.keySet()) {
                Map<String, Integer> execution = executionMapByAnonymous.get(templateName);
                if (execution.containsKey(sessionId)) {
                    double accessLn = Math.log(execution.get(sessionId) + 1);
                    lnAnonymousMap.put(templateName, accessLn);
                }
            }
        }
        return lnAnonymousMap;
    }
}
