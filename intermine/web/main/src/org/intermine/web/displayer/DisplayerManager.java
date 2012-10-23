package org.intermine.web.displayer;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.config.WebConfig;

/**
 * Read in and provide access to report page displayers.  Displayers are constructed based
 * on entries in webconfig-model.xml and cached.  On creation config is copied to subclasses.
 * @author Richard Smith
 *
 */
public final class DisplayerManager
{
    private static DisplayerManager instance = null;
    private Map<String, Map<String, List<ReportDisplayer>>> displayers =
        new HashMap<String, Map<String, List<ReportDisplayer>>>();
    private static final String DEFAULT_PLACEMENT = "summary";

    protected static final Logger LOG = Logger.getLogger(DisplayerManager.class);

    /**
     * Fetch the DisplayerManager, a single instance is held.
     * @param webConfig web configuration
     * @param im the InterMine API
     * @return the DisplayerManager
     */
    public static DisplayerManager getInstance(WebConfig webConfig, InterMineAPI im) {
        if (instance == null) {
            instance = new DisplayerManager(webConfig, im);
        }
        return instance;
    }

    private DisplayerManager(WebConfig webConfig, InterMineAPI im) {
        for (ReportDisplayerConfig config : webConfig.getReportDisplayerConfigs()) {

            ReportDisplayer displayer = null;
            try {
                String reportDisplayerName = config.getJavaClass();

                Class<?> clazz = TypeUtil.instantiate(reportDisplayerName);
                Constructor<?> m = clazz.getConstructor(
                        new Class[] {ReportDisplayerConfig.class, InterMineAPI.class});
                displayer = (ReportDisplayer) m.newInstance(new Object[] {config, im});
            } catch (Exception e) {
                LOG.error("Failed to instantiate displayer for class: " + config.getJavaClass()
                        + " because: " + e);
            }
            if (displayer == null) {
                continue;
            }

            String placement = config.getPlacement();
            if (placement == null) {
                placement = DEFAULT_PLACEMENT;
            }
            Set<String> allTypes = new HashSet<String>();
            for (String type : config.getConfiguredTypes()) {
                ClassDescriptor cld = im.getModel().getClassDescriptorByName(type);
                if (cld != null) {
                    allTypes.add(type);
                    for (ClassDescriptor sub : im.getModel().getAllSubs(cld)) {
                        allTypes.add(sub.getUnqualifiedName());
                    }
                } else {
                    LOG.error("The type " + type + " does not exist, check webconfig-model.xml.");
                }
            }
            for (String type : allTypes) {
                Map<String, List<ReportDisplayer>> typeDisplayers = displayers.get(type);
                if (typeDisplayers == null) {
                    typeDisplayers = new HashMap<String, List<ReportDisplayer>>();
                    displayers.put(type, typeDisplayers);
                }
                List<ReportDisplayer> placementDisplayers = typeDisplayers.get(placement);
                if (placementDisplayers == null) {
                    placementDisplayers = new ArrayList<ReportDisplayer>();
                    typeDisplayers.put(placement, placementDisplayers);
                }
                placementDisplayers.add(displayer);
            }
        }
    }

    /**
     * Get all displayers for the given type regardless of placement.
     * @param type an unqualified class name to look up
     * @return a set of displayers or an empty set if there are none
     */
    public Set<ReportDisplayer> getAllReportDisplayersForType(String type) {
        Set<ReportDisplayer> displayersForType = new HashSet<ReportDisplayer>();
        if (displayers.containsKey(type)) {
            for (List<ReportDisplayer> disps : displayers.get(type).values()) {
                displayersForType.addAll(disps);
            }
        }
        return displayersForType;
    }

    /**
     * Get a map from placement string (a data category or summary) to displayers for the given
     * type.  Returns null if there are no displayers for the type.
     * @param type an unqualified class name
     * @return a map from placement to displayers or null
     */
    public Map<String, List<ReportDisplayer>> getReportDisplayersForType(String type) {
        return displayers.get(type);
    }

    /**
     * Get a specific ReportDisplayer by its name for a given ReportObject type
     * @param objectType object type (Gene etc)
     * @param name of the displayer
     * @return Displayer
     */
    public ReportDisplayer getReportDisplayerByName(String objectType, String name) {
        for (List<ReportDisplayer> l : displayers.get(objectType).values()) {
            for (ReportDisplayer d : l) {
                if (d.getDisplayerName().equals(name)) {
                    return d;
                }
            }
        }
        return null;
    }

}
