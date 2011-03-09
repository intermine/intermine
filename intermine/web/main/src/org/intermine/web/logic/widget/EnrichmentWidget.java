package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;

/**
 * @author "Xavier Watkins"
 *
 */
public class EnrichmentWidget extends Widget
{

    private int notAnalysed = 0;
    private InterMineBag bag;
    private ObjectStore os;
    private String filter;
    private ArrayList<Map> resultMaps = new ArrayList<Map>();
    private String errorCorrection, max;


    /**
     * @param config widget config
     * @param interMineBag bag for this widget
     * @param os object store
     * @param errorCorrection which error correction to use (Bonferroni, etc)
     * @param max maximum value to display (0 - 1)
     * @param filter filter to use (ie Ontology)
     */
    public EnrichmentWidget(EnrichmentWidgetConfig config, InterMineBag interMineBag,
                            ObjectStore os, String filter, String max, String errorCorrection) {
        super(config);
        this.bag = interMineBag;
        this.os = os;
        this.errorCorrection = errorCorrection;
        this.max = max;
        this.filter = filter;
        process();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process() {
        try {
            Class<?> clazz = TypeUtil.instantiate(config.getDataSetLoader());
            Constructor<?> constr = clazz.getConstructor(new Class[] {InterMineBag.class,
                ObjectStore.class, String.class});

            EnrichmentWidgetLdr ldr = (EnrichmentWidgetLdr) constr
                .newInstance(new Object[] {bag, os, filter});
            resultMaps = WidgetUtil.statsCalc(os, ldr, bag, new Double(0 + max),
                    errorCorrection);
            int analysedTotal = 0;
            if (!resultMaps.isEmpty()) {
                analysedTotal = ((Integer) (resultMaps.get(3)).get("widgetTotal")).intValue();
            }
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
     * {@inheritDoc}
     */
    public List getElementInList() {
        return new Vector();
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

    /**
     * {@inheritDoc}
     */
    public boolean getHasResults() {
        return (resultMaps.get(0) != null && resultMaps.get(0).size() > 0);
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String>> getExportResults(String[]selected) throws Exception {

        Map<String, BigDecimal> pvalues = resultMaps.get(0);
        //Map<String, Long> totals = resultMaps.get(1);
        Map<String, String> labelToId = resultMaps.get(2);
        List<List<String>> exportResults = new ArrayList<List<String>>();
        List<String> selectedIds = Arrays.asList(selected);

        Class<?> clazz = TypeUtil.instantiate(config.getDataSetLoader());
        Constructor<?> constr = clazz.getConstructor(new Class[] {InterMineBag.class,
            ObjectStore.class, String.class});

        EnrichmentWidgetLdr ldr = (EnrichmentWidgetLdr) constr.newInstance(new Object[] {bag, os,
            filter});

        Query q = ldr.getExportQuery(selectedIds);

        Results res = os.execute(q);
        Iterator iter = res.iterator();
        HashMap<String, List<String>> termsToIds = new HashMap();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            String termId = resRow.get(0).toString();
            String id = resRow.get(1).toString();
            if (!termsToIds.containsKey(termId)) {
                termsToIds.put(termId, new ArrayList<String>());
            }
            termsToIds.get(termId).add(id);
        }

        for (String id : selectedIds) {
            if (labelToId.get(id) != null) {

                List row = new LinkedList();
                row.add(id);
                String label = labelToId.get(id);
                if (!label.equals(id)) {
                    row.add(label);
                }

                BigDecimal bd = pvalues.get(id);
                row.add(new Double(bd.doubleValue()));

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
     * {@inheritDoc}
     */
    public List<List<String[]>> getFlattenedResults() {
        if (resultMaps != null && !resultMaps.isEmpty()) {
            Map<String, BigDecimal> pvalues = resultMaps.get(0);
            Map<String, Long> totals = resultMaps.get(1);
            Map<String, String> labelToId = resultMaps.get(2);
            List<List<String[]>> flattenedResults = new LinkedList<List<String[]>>();
            for (String id : pvalues.keySet()) {
                List<String[]> row = new LinkedList<String[]>();

                row.add(new String[] {"<input name=\"selected\" value=\"" + id
                        + "\" id=\"selected_" + id + "\" type=\"checkbox\">"});

                String label = labelToId.get(id);
                if (config.getExternalLink() != null && !"".equals(config.getExternalLink())) {
                    label += " <a href=\"" + config.getExternalLink() + id
                             + "\" target=\"_new\" class=\"extlink\">[";
                    if (config.getExternalLinkLabel() != null
                        && !"".equals(config.getExternalLinkLabel())) {
                        label += config.getExternalLinkLabel();
                    }
                    label += id + "]</a>";
                }
                row.add(new String[] {label});

                BigDecimal bd = pvalues.get(id);
                if (bd.compareTo(new BigDecimal(0.00000099)) <= 0) {
                    NumberFormat formatter = new DecimalFormat();
                    formatter = new DecimalFormat("0.####E0");
                    row.add(new String[] {formatter.format(bd)});
                } else {
                    row.add(new String[] {bd.setScale(7, BigDecimal.ROUND_HALF_EVEN)
                            .toEngineeringString()});
                }

                row.add(new String[] {totals.get(id).toString(), "widgetAction.do?key=" + id
                        + "&bagName=" + bag.getName() + "&link=" + config.getLink()});
                flattenedResults.add(row);
            }
            return flattenedResults;
        }
        return null;
    }

    /**
     *
     * @return List of column labels
     */
    public List<String> getColumns() {
        return Arrays.asList(new String[] {((EnrichmentWidgetConfig) config).getLabel(), "p-Value",
            ""});
    }
}
