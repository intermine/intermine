package org.intermine.bio.web.displayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Displayer for gene sequence feature
 * @author rns, radek
 *
 */
public class SampleGeoDisplayer extends ReportDisplayer
{
    /** @var sets the max number of locations to show in a table, TODO: match with DisplayObj*/
    private Integer maximumNumberOfLocations = 27;

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public SampleGeoDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void display(HttpServletRequest request, ReportObject reportObject) {
        InterMineObject imObj = reportObject.getObject();
        Object loc = null;

        String imoClassName = imObj.getClass().getSimpleName();
        if (imoClassName.endsWith("Shadow")) {
            imoClassName = imoClassName.substring(0, imoClassName.indexOf("Shadow"));
        }
        request.setAttribute("objectClass", imoClassName);

        // check for description/preparation/lattitude/longitude/elevation fields and display it
        String[] stringFields = {"name","description","library"};
        for( String field : stringFields ) {
          try {
              String value = (String) imObj.getFieldValue(field);
              if (!StringUtils.isBlank(value)) {
                  request.setAttribute(field, value);
              }
          } catch (IllegalAccessException e) {
              // we'll quietly ignore missing fields.
          }
        }
        String[] doubleFields = {"latitude","longitude","elevation"};
        for( String field : doubleFields ) {
          try {
              Double value = (Double) imObj.getFieldValue(field);
              if (value != null) {
                  request.setAttribute(field, value.toString());
              }
          } catch (IllegalAccessException e) {
              // we'll quietly ignore missing fields.
          }
        }
    }
}
