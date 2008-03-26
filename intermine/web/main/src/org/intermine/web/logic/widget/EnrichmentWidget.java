package org.intermine.web.logic.widget;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Julie Sullivan
 */
public class EnrichmentWidget extends Widget
{
    private String max, filters, filterLabel, errorCorrection;
    private String label, externalLink;
    private ArrayList<Map> results;
    private InterMineBag bag;

    
    public void process(InterMineBag bag, ObjectStore os) {
        try {
            // set bag
            this.bag = bag;
            Class<?> clazz = TypeUtil.instantiate(getDataSetLoader());
            Constructor<?> constr = clazz.getConstructor(new Class[]
                                                                {
                InterMineBag.class, ObjectStore.class, String.class
                                                                });

            EnrichmentWidgetLdr ldr = (EnrichmentWidgetLdr) constr.newInstance(new Object[]
                                                                                          {
                bag, os, getSelectedExtraAttribute()
                                                                                          });
            // have to calculate sample total for each enrichment widget because namespace may have
            // changed
            results = WebUtil.statsCalc(os, ldr.getAnnotatedPopulation(),
                                                       ldr.getAnnotatedSample(), 
                                                       getTotal(os, ldr, false),
                                                       getTotal(os, ldr, true),
                                                       bag,
                                                       new Double(0 
                                                       + max),
                                                       errorCorrection);
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private int getTotal(ObjectStore os, EnrichmentWidgetLdr ldr, boolean useBag) {

        int n = 0;

        Query q = ldr.getQuery(true, useBag);

        Results r = os.execute(q);
        if (!r.isEmpty()) {
            Iterator<ResultsRow> it = r.iterator();
            ResultsRow rr =  it.next();
            Long l = (java.lang.Long) rr.get(0);
            n = l.intValue();
        }

        return n;
    }
    
    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the filters
     */
    public String getFilters() {
        return filters;
    }

    /**
     * @param filters list of filters to display on the widget
     */
    public void setFilters(String filters) {
        this.filters = filters;
    }

    /**
     * @return the label for the filters
     */
    public String getFilterLabel() {
        return filterLabel;
    }

    /**
     * @param filterLabel the label for the filters
     */
    public void setFilterLabel(String filterLabel) {
        this.filterLabel = filterLabel;
    }

    /**
     * @return the maximum value this widget will display
     */
    public String getMax() {
        return max;
    }

    /**
     * @param max maximum value this widget will display
     */
    public void setMax(String max) {
        this.max = max;
    }


    /**
     * Return an XML String of this Type object
     * @return a String version of this WebConfig object
     */
    public String toString() {
        return "< title=\"" + getTitle() + "\" link=\"" + getLink() + "\" ldr=\""
               + getDataSetLoader() + "\"/>";
    }


    /**
     * @return the externalLink
     */
    public String getExternalLink() {
        return externalLink;
    }

    /**
     * @param externalLink the externalLink to set
     */
    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }
    
    /**
     * {@inheritDoc}
     */
    public Map<String, Collection> getExtraAttributes(InterMineBag imBag, ObjectStore os) {
        Map<String, Collection> returnMap = new HashMap<String, Collection>();
        returnMap.put(getFilterLabel(), Arrays.asList(getFilters().split(",")));
        return returnMap;
    }
    
    /**
     * 
     * @return List of column labels
     */
    public List getColumns() {
        return Arrays.asList(new String[]
            {
                "", label, "p-Value", ""
            });
    }
    
    public List<List<String[]>> getFlattenedResults() {
        if (results != null && !results.isEmpty()) {
            Map<String, BigDecimal> pvalues = results.get(0);
            Map<String, Long> totals = results.get(1);
            Map<String, String> labelToId = results.get(2);
            List<List<String[]>> flattenedResults = new LinkedList<List<String[]>>();
            for (String id : pvalues.keySet()) {
                List<String[]> row = new LinkedList();
                BigDecimal bd = pvalues.get(id);
                row.add(new String[]
                    {
                        "<input name=\"selected\" value=\"" + id + "\" id=\"selected_" + id
                                        + "\" type=\"checkbox\">"
                    });
                row.add(new String[] {labelToId.get(id)});
                
                row.add(new String[] {bd.setScale(7, 
                BigDecimal.ROUND_HALF_EVEN).toEngineeringString()});
                row.add(new String[]
                    {
                        totals.get(id).toString(),
                        "widgetAction.do?key=" + id + "&bagName=" + bag.getName() + "&link="
                                        + getLink()
                    });
                flattenedResults.add(row);
            }
            return flattenedResults;            
//            ("ewf", ewf);
//            ("pvalues", results.get(0));
//            ("totals", results.get(1));
//            ("labelToId", results.get(2));
//            ("referencePopulation", "All " + bag.getType() + "s from  "
//                                 + StringUtil.prettyList(ldr.getPopulationDescr(), true));
        }
        return null;
    }
    
    /**
     * Get the results in an exportable format for the specified ids
     * @param selected the selected ids to export
     * @return a list of list of Strings
     */
    public List<List<String>> getExportResults(String[]selected) {
        Map<String, BigDecimal> pvalues = results.get(0);
        Map<String, Long> totals = results.get(1);
        Map<String, String> labelToId = results.get(2);
        List<List<String>> exportResults = new ArrayList<List<String>>();
        List<String> selectedIds = Arrays.asList(selected);
        for (String id : pvalues.keySet()) {
            if (selectedIds.contains(id)) {
                List<String> row = new LinkedList();
                row.add(labelToId.get(id));
                BigDecimal bd = pvalues.get(id);
                row.add(bd.setScale(7, BigDecimal.ROUND_HALF_EVEN).toEngineeringString());
                row.add(totals.get(id).toString());
                exportResults.add(row);
            }
        }
        return exportResults;
    }

    /**
     * @return the errorCorrection
     */
    public String getErrorCorrection() {
        return errorCorrection;
    }

    /**
     * @param errorCorrection the errorCorrection to set
     */
    public void setErrorCorrection(String errorCorrection) {
        this.errorCorrection = errorCorrection;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean getHasResults() {
        return (results != null && results.size() > 0);
    }
    
}
