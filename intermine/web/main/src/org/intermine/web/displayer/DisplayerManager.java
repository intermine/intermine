package org.intermine.web.displayer;

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

public class DisplayerManager {

    private static DisplayerManager instance = null;

    protected static final Logger LOG = Logger.getLogger(DisplayerManager.class);

    private Map<String, Map<String, List<CustomDisplayer>>> displayers =
        new HashMap<String, Map<String, List<CustomDisplayer>>>();

    public static DisplayerManager getInstance(WebConfig webConfig, InterMineAPI im) {
        if (instance == null) {
            instance = new DisplayerManager(webConfig, im);
        }
        return instance;
    }

    protected DisplayerManager(WebConfig webConfig, InterMineAPI im) {
        for (ReportDisplayerConfig config : webConfig.getReportDisplayerConfigs()) {

            CustomDisplayer displayer = null;
            try {
                String customDisplayerName = config.getJavaClass();

                Class<?> clazz =
                    TypeUtil.instantiate(customDisplayerName);
                Constructor m = clazz.getConstructor(
                        new Class[] {ReportDisplayerConfig.class, InterMineAPI.class});
                displayer = (CustomDisplayer) m.newInstance(new Object[] {config, im});
            } catch (Exception e) {
                LOG.error("Failed to instantiate displayer for class: " + config.getJavaClass() +
                        " because: " + e);
            }
            if (displayer == null) {
                continue;
            }

            String placement = config.getPlacement();
            // TODO formalise default placements
            if (placement == null) {
                placement = "summary";
            }
            Set<String> allTypes = new HashSet<String>();
            for (String type : config.getConfiguredTypes()) {
                ClassDescriptor cld = im.getModel().getClassDescriptorByName(type);
                allTypes.add(type);
                for (ClassDescriptor sub : im.getModel().getAllSubs(cld)) {
                    allTypes.add(sub.getUnqualifiedName());
                }
            }
            for (String type : allTypes) {
                Map<String, List<CustomDisplayer>> typeDisplayers = displayers.get(type);
                if (typeDisplayers == null) {
                    typeDisplayers = new HashMap<String, List<CustomDisplayer>>();
                    displayers.put(type, typeDisplayers);
                }
                List<CustomDisplayer> placementDisplayers = typeDisplayers.get(placement);
                if (placementDisplayers == null) {
                    placementDisplayers = new ArrayList<CustomDisplayer>();
                    typeDisplayers.put(placement, placementDisplayers);
                }
                placementDisplayers.add(displayer);
            }
        }
    }

    public Set<CustomDisplayer> getAllReportDisplayersForType(String type) {
        Set<CustomDisplayer> displayersForType = new HashSet<CustomDisplayer>();
        if (displayers.containsKey(type)) {
            for (List<CustomDisplayer> disps : displayers.get(type).values()) {
                displayersForType.addAll(disps);
            }
        }
        return displayersForType;
    }

    public Map<String, List<CustomDisplayer>> getReportDisplayersForType(String type) {
        return displayers.get(type);
    }

}
