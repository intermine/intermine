package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ResultElement;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.DisplayCollection;
import org.intermine.web.logic.results.DisplayField;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.InlineResultsTable;
import org.intermine.web.logic.results.InlineResultsTableRow;

public class SequenceFeatureDisplayer extends CustomDisplayer {

    /** @var List with a path (in placementRefsAndCollections) to locations Collection */
    private ArrayList<String> locationCollectionPathlist = new ArrayList<String>() {
        {
            add("im:aspect:Miscellaneous");
            add("locations");
        }
    };

    public SequenceFeatureDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    public void display(HttpServletRequest request, DisplayObject displayObject) {
        InterMineObject imObj = displayObject.getObject();
        Object loc = null;
        try {
            loc = imObj.getFieldValue("chromosomeLocation");
            // if the chromosomeLocation reference is null iterate over the contents of the
            //  locations collection and display each location where the locatedOn reference points
            //  to a Chromosome object
            if (loc == null) {
                Map<String, Map<String, DisplayField>> map
                    = (Map<String, Map<String, DisplayField>>)
                    request.getAttribute("placementRefsAndCollections");

                if (map.containsKey(locationCollectionPathlist.get(0))) {
                    if (map.get(locationCollectionPathlist.get(0)).containsKey(
                            locationCollectionPathlist.get(1))) {
                        DisplayCollection df = (DisplayCollection) map.get(
                                locationCollectionPathlist.get(0)).get(
                                locationCollectionPathlist.get(1));
                        // now we have a Collection corresponding to "Misc > locations"
                        Collection col = df.getCollection();
                        List results = new ArrayList();
                        for (Object item : col) {
                            InterMineObject imLocation = (InterMineObject) item;
                            // fetch where this object is located
                            Object locatedOnObject = imLocation.getFieldValue("locatedOn");
                            if (locatedOnObject != null) {
                                // are we Chromosome?
                                if ("Chromosome".equals(DynamicUtil.getSimpleClass(
                                        (InterMineObject) locatedOnObject).getSimpleName())) {
                                    results.add(item);
                                }
                            }
                        }

                        request.setAttribute("col", results);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
