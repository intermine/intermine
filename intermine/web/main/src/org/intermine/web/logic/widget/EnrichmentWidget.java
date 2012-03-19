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

import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;

/**
 * @author "Xavier Watkins"
 *
 */
public class EnrichmentWidget extends Widget
{

    private static final Logger LOG = Logger.getLogger(EnrichmentWidget.class);
    private int notAnalysed = 0;
    private InterMineBag bag;
    private ObjectStore os;
    private String filter;
    private EnrichmentResults results;
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
            EnrichmentWidgetLdr ldr = new EnrichmentWidgetImplLdr(bag, os,
                (EnrichmentWidgetConfig) config, filter);
            EnrichmentInput input = new EnrichmentInputWidgetLdr(os, ldr);
            Double maxValue = Double.parseDouble(max);
            results = EnrichmentCalculation.calculate(input, maxValue, errorCorrection);
            setNotAnalysed(bag.getSize() - results.getAnalysedTotal());
        } catch (ObjectStoreException e) {
            // TODO Auto-generated catch block
            LOG.error(e.getMessage(), e);
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            LOG.error(e.getMessage(), e);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            LOG.error(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
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
        return results.getPValues().size() > 0;
    }

    private Map<String, List<String>> getTermsToIdsForExport(List<String> selectedIds) throws Exception {
        EnrichmentWidgetLdr ldr = new EnrichmentWidgetImplLdr(bag, os,
                (EnrichmentWidgetConfig) config, filter);

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

        return termsToIds;
    }

    private Map<String, List<Map<String, Object>>> getTermsToIds(List<String> selectedIds) throws Exception {
        EnrichmentWidgetLdr ldr = new EnrichmentWidgetImplLdr(bag, os,
                (EnrichmentWidgetConfig) config, filter);

        Query q = ldr.getExportQuery(selectedIds);

        Results res = os.execute(q);
        Iterator iter = res.iterator();
        HashMap<String, List<Map<String, Object>>> termsToIds = new HashMap();
        while (iter.hasNext()) {
            ResultsRow resRow = (ResultsRow) iter.next();
            String termId = resRow.get(0).toString();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("displayed", resRow.get(1).toString());
            map.put("id", resRow.get(2).toString());
            if (!termsToIds.containsKey(termId)) {
                termsToIds.put(termId, new ArrayList<Map<String,Object>>());
            }
            termsToIds.get(termId).add(map);
        }

        return termsToIds;
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String>> getExportResults(String[] selected) throws Exception {

        Map<String, BigDecimal> pValues = results.getPValues();
        Map<String, String> labels = results.getLabels();
        List<List<String>> exportResults = new ArrayList<List<String>>();
        List<String> selectedIds = Arrays.asList(selected);

        Map<String, List<String>> termsToIds = getTermsToIdsForExport(selectedIds);

        for (String id : selectedIds) {
            if (labels.get(id) != null) {

                List row = new LinkedList();
                row.add(id);
                String label = labels.get(id);
                if (!label.equals(id)) {
                    row.add(label);
                }

                BigDecimal bd = pValues.get(id);
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
        if (results != null) {
            Map<String, BigDecimal> pValues = results.getPValues();
            Map<String, Integer> counts = results.getCounts();
            Map<String, String> labels = results.getLabels();
            List<List<String[]>> flattenedResults = new LinkedList<List<String[]>>();
            for (String id : pValues.keySet()) {
                List<String[]> row = new LinkedList<String[]>();

                row.add(new String[] {"<input name=\"selected\" value=\"" + id
                        + "\" id=\"selected_" + id + "\" type=\"checkbox\">"});

                String label = labels.get(id);
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

                BigDecimal bd = pValues.get(id);
                if (bd.compareTo(new BigDecimal(0.00000099)) <= 0) {
                    NumberFormat formatter = new DecimalFormat();
                    formatter = new DecimalFormat("0.####E0");
                    row.add(new String[] {formatter.format(bd)});
                } else {
                    row.add(new String[] {bd.setScale(7, BigDecimal.ROUND_HALF_EVEN)
                            .toEngineeringString()});
                }

                row.add(new String[] {counts.get(id).toString(), "widgetAction.do?key=" + id
                        + "&bagName=" + bag.getName() + "&link=" + config.getLink()});
                flattenedResults.add(row);
            }
            return flattenedResults;
        }
        return null;
    }

    public List<List<Object>> getResults() throws Exception {
        List<List<Object>> exportResults = new LinkedList<List<Object>>();
        if (results != null) {
            Map<String, BigDecimal> pValues = results.getPValues();
            Map<String, Integer> counts = results.getCounts();
            Map<String, String> labels = results.getLabels();
            for (String id : pValues.keySet()) {
                List<Object> row = new LinkedList<Object>();
                row.add(id);
                row.add(labels.get(id));
                row.add(pValues.get(id).doubleValue());
                row.add(counts.get(id));
                Map<String, List<Map<String, Object>>> termsToIds = getTermsToIds(Arrays.asList(id));
                row.add(termsToIds.get(id));
                exportResults.add(row);
            }
        }
        return exportResults;
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
