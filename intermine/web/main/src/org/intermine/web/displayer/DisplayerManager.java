package org.intermine.web.displayer;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.config.WebConfig;

public class DisplayerManager {

    private static DisplayerManager instance = null;

    protected static final Logger LOG = Logger.getLogger(DisplayerManager.class);
    
    private Map<String, Map<String, Set<CustomDisplayer>>> displayers =
        new HashMap<String, Map<String, Set<CustomDisplayer>>>();

    public static DisplayerManager getInstance(WebConfig webConfig, Model model) {
        if (instance == null) {
            instance = new DisplayerManager(webConfig, model);
        }
        return instance;
    }

    protected DisplayerManager(WebConfig webConfig, Model model) {
        for (ReportDisplayerConfig config : webConfig.getReportDisplayerConfigs()) {
            
            CustomDisplayer displayer = null;
            try {
                String customDisplayerName = config.getJavaClass();
                
                Class<?> clazz =
                    TypeUtil.instantiate(customDisplayerName);
                Constructor m = clazz.getConstructor(new Class[] {ReportDisplayerConfig.class});
                displayer = (CustomDisplayer) m.newInstance(new Object[] {config});
                //displayers.put(customDisplayerName, displayer);
            } catch (Exception e) {
                LOG.error("Failed to instantiate displayer: " + e);
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
                ClassDescriptor cld = model.getClassDescriptorByName(type);
                allTypes.add(type);
                for (ClassDescriptor sub : model.getAllSubs(cld)) {
                    allTypes.add(sub.getUnqualifiedName());
                }
            }
            for (String type : allTypes) {
                Map<String, Set<CustomDisplayer>> typeDisplayers = displayers.get(type);
                if (typeDisplayers == null) {
                    typeDisplayers = new HashMap<String, Set<CustomDisplayer>>();
                    displayers.put(type, typeDisplayers);
                }
                Set<CustomDisplayer> placementDisplayers = typeDisplayers.get(placement);
                if (placementDisplayers == null) {
                    placementDisplayers = new HashSet<CustomDisplayer>();
                    typeDisplayers.put(placement, placementDisplayers);
                }
                placementDisplayers.add(displayer);
            }
        }
    }

    public Set<CustomDisplayer> getAllReportDislayersForType(String type) {
        Set<CustomDisplayer> displayersForType = new HashSet<CustomDisplayer>();
        if (displayers.containsKey(type)) {
            for (Set<CustomDisplayer> disps : displayers.get(type).values()) {
                displayersForType.addAll(disps);
            }
        }
        return displayersForType;
    }

}
