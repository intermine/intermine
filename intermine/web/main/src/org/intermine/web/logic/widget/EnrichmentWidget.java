package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;

/**
 * @author Julie Sullivan
 */
public class EnrichmentWidget extends Widget
{
    private String max, filters, filterLabel, errorCorrection;
    private String label;
    private String externalLink, externalLinkLabel;
    private String append;
    private ArrayList<Map> resultMaps = new ArrayList<Map>();
    private InterMineBag bag;
    private int notAnalysed;
    private ObjectStore os;

    /**
     * {@inheritDoc}
     */
    public void process(InterMineBag imbag, ObjectStore ost) {
        try {
            this.bag = imbag;
            this.os = ost;
            Class<?> clazz = TypeUtil.instantiate(getDataSetLoader());
            Constructor<?> constr = clazz.getConstructor(new Class[]
                                                                {
                InterMineBag.class, ObjectStore.class, String.class
                                                                });

            EnrichmentWidgetLdr ldr = (EnrichmentWidgetLdr) constr.newInstance(new Object[]
                                                                                          {
                bag, os, getSelectedExtraAttribute()
                                                                                          });

            resultMaps = WebUtil.statsCalc(os, ldr, bag, new Double(0 + max), errorCorrection);

            int analysedTotal = ((Integer) (resultMaps.get(3)).get("widgetTotal")).intValue();

            setNotAnalysed(bag.getSize() - analysedTotal);

        } catch (ObjectStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
     * {@inheritDoc}
     */
    public String getExternalLink() {
        return externalLink;
    }

    /**
     * {@inheritDoc}
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
    public List<String> getColumns() {
        return Arrays.asList(new String[]
            {
                label, "p-Value", ""
            });
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String[]>> getFlattenedResults() {
        if (resultMaps != null && !resultMaps.isEmpty()) {
            Map<String, BigDecimal> pvalues = resultMaps.get(0);
            Map<String, Long> totals = resultMaps.get(1);
            Map<String, String> labelToId = resultMaps.get(2);
            List<List<String[]>> flattenedResults = new LinkedList<List<String[]>>();
            for (String id : pvalues.keySet()) {
                List<String[]> row = new LinkedList();
                BigDecimal bd = pvalues.get(id);
                row.add(new String[]
                    {
                        "<input name=\"selected\" value=\"" + id + "\" id=\"selected_" + id
                                        + "\" type=\"checkbox\">"
                    });

                String label = labelToId.get(id);
                if (externalLink != null && !externalLink.equals("")) {
                    label += " <a href=\"" + externalLink + id
                             + "\" target=\"_new\" class=\"extlink\">[";
                     if (externalLinkLabel != null && !externalLinkLabel.equals("")) {
                         label += externalLinkLabel;
                     }
                     label += id + "]</a>";
                }
                row.add(new String[] {label});

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
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String>> getExportResults(String[]selected) throws Exception {

        Map<String, BigDecimal> pvalues = resultMaps.get(0);
        Map<String, Long> totals = resultMaps.get(1);
        Map<String, String> labelToId = resultMaps.get(2);
        List<List<String>> exportResults = new ArrayList<List<String>>();
        List<String> selectedIds = Arrays.asList(selected);

        Class<?> clazz = TypeUtil.instantiate(getDataSetLoader());
        Constructor<?> constr = clazz.getConstructor(new Class[]
                                                            {
            InterMineBag.class, ObjectStore.class, String.class
                                                            });

        EnrichmentWidgetLdr ldr = (EnrichmentWidgetLdr) constr.newInstance(new Object[]
                                                                                      {
            bag, os, getSelectedExtraAttribute()
                                                                                      });
        Model model = os.getModel();
        Class<?> bagCls = Class.forName(model.getPackageName() + "." + bag.getType());
        QueryClass qc = new QueryClass(bagCls);

        Query q = ldr.getExportQuery(selectedIds);

        Results res = os.execute(q);
        Iterator iter = res.iterator();
        HashMap<String, List<String>> termsToIds = new HashMap();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            String term = resRow.get(0).toString();
            String id = resRow.get(1).toString();
            if (!termsToIds.containsKey(term)) {
                termsToIds.put(term, new ArrayList<String>());
            }
            termsToIds.get(term).add(id);
        }

        for (String id : selectedIds) {
            if (labelToId.get(id) != null) {
                List row = new LinkedList();

                row.add(id);

                BigDecimal bd = pvalues.get(id);
                Double d = bd.doubleValue();
                row.add(d);

                List<String> ids = termsToIds.get(id);
                StringBuffer sb = new StringBuffer();
                for (String term : ids) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(term);
                }
                row.add(sb.toString());

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
        return (resultMaps.get(0) != null && resultMaps.get(0).size() > 0);
    }

    /**
     * just used for tiffin for now
     * @return the text to append to the end of the external link
     */
    public String getAppend() {
        return append;
    }

    /**
     * @param append the text to append
     */
    public void setAppend(String append) {
        this.append = append;
    }

    /**
     * @return the externalLinkLabel
     */
    public String getExternalLinkLabel() {
        return externalLinkLabel;
    }

    /**
     * @param externalLinkLabel the externalLinkLabel to set
     */
    public void setExternalLinkLabel(String externalLinkLabel) {
        this.externalLinkLabel = externalLinkLabel;
    }

    /**
     * {@inheritDoc}
     */
    public int getNotAnalysed() {
        return notAnalysed;
    }

    /**
    * {@inheritDoc}
     */
    public void setNotAnalysed(int notAnalysed) {
        this.notAnalysed = notAnalysed;
    }
}
