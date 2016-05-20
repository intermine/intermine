package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;

public class EnrichmentWidgetTest extends WidgetConfigTestCase
{
    private final class FixureOptions implements EnrichmentOptions {
        @Override
        public String getFilter() {
            return filter;
        }

        @Override
        public double getMaxPValue() {
            return 1.0d;
        }

        @Override
        public String getCorrection() {
            return CORRECTION;
        }

        @Override
        public String getExtraCorrectionCoefficient() {
            return null;
        }
    }

    private EnrichmentWidget widget;
    private String MAX = "1.0";
    private String CORRECTION = "Bonferroni";

    private InterMineBag bag;
    private WidgetConfig config;
    private String filter;
    private EnrichmentResults results;
    EnrichmentOptions options;

    public void setUp() throws Exception {
        super.setUp();
        config = webConfig.getWidgets().get("contractor_enrichment_with_filter1");
        InterMineBag employeeList = createEmployeeList();
        bag = employeeList;
        options = new FixureOptions();
        widget = new EnrichmentWidget((EnrichmentWidgetConfig) config, bag, null, os, options,
                null, null);
    }

    public void testValidateBagType() throws Exception {
        InterMineBag companyList = createCompanyList();
        WidgetConfig config = webConfig.getWidgets().get("contractor_enrichment_with_filter1");
        try {
            EnrichmentWidget w = new EnrichmentWidget(
                    (EnrichmentWidgetConfig) config, companyList, null, os, options,
                    null, null);
            w.process();
            fail("Should raise a IllegalArgumentException");
        } catch (IllegalArgumentException iae){
        }
    }

    public void testProcess() throws Exception {
        EnrichmentWidgetImplLdr ldr
            = new EnrichmentWidgetImplLdr(bag, null, os, (EnrichmentWidgetConfig) config, filter, false, null,
                    null, null);
        EnrichmentInput input = new EnrichmentInputWidgetLdr(os, ldr);
        Double maxValue = Double.parseDouble(MAX);
        results = EnrichmentCalculation.calculate(input, maxValue, CORRECTION, false, null);

        List<List<Object>> exportResults = getResults();

        assertEquals(1, exportResults.size());       // there is only one contract
        assertEquals(2, exportResults.get(0).get(3));// 2 employees match with the only contracts
    }

    public void testProcessLCBonferroni() throws Exception {
        EnrichmentWidgetImplLdr ldr
            = new EnrichmentWidgetImplLdr(bag, null, os, (EnrichmentWidgetConfig) config, filter, false, null,
                    null, null);
        EnrichmentInput input = new EnrichmentInputWidgetLdr(os, ldr);
        Double maxValue = Double.parseDouble(MAX);
        results = EnrichmentCalculation.calculate(input, maxValue, CORRECTION.toLowerCase(), false, null);

        List<List<Object>> exportResults = getResults();

        assertEquals(1, exportResults.size());       // there is only one contract
        assertEquals(2, exportResults.get(0).get(3));// 2 employees match with the only contracts
    }

    public void testProcessHolmBonferroni() throws Exception {
        EnrichmentWidgetImplLdr ldr
            = new EnrichmentWidgetImplLdr(bag, null, os, (EnrichmentWidgetConfig) config, filter, false, null,
                    null, null);
        EnrichmentInput input = new EnrichmentInputWidgetLdr(os, ldr);
        Double maxValue = Double.parseDouble(MAX);
        results = EnrichmentCalculation.calculate(input, maxValue, "Holm-Bonferroni", false, null);

        List<List<Object>> exportResults = getResults();

        assertEquals(1, exportResults.size());       // there is only one contract
        assertEquals(2, exportResults.get(0).get(3));// 2 employees match with the only contracts
    }

    public boolean getHasResults() {
        widget.process();
        return results.getPValues().size() > 0;
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
                exportResults.add(row);
            }
        }
        return exportResults;
    }

    public void testGetPathQuery() {
        PathQuery q = new PathQuery(os.getModel());
        q.addView("Employee.name");
        q.addView("Employee.age");
        q.addView("Employee.department.name");
        // bag constraint
        q.addConstraint(Constraints.in(config.getStartClass(), bag.getName()));
        assertEquals(q, widget.getPathQuery());
    }

    public void testGetPathQueryForMatches() {
        Model model = os.getModel();
        PathQuery pathQuery = new PathQuery(model);
        pathQuery.addView("Employee.department.company.contractors.name");
        pathQuery.addView("Employee.name");
        pathQuery.addOrderBy("Employee.department.company.contractors.name", OrderDirection.ASC);
        pathQuery.addConstraint(Constraints.in(config.getStartClass(), bag.getName()));

        assertEquals(pathQuery, widget.getPathQueryForMatches());
    }

    public void testCreatePathQueryView() {
        PathQuery pathQuery = new PathQuery(os.getModel());
        pathQuery.addView("Employee.name");
        pathQuery.addView("Employee.age");
        pathQuery.addView("Employee.department.name");
        assertEquals(pathQuery, widget.createPathQueryView(os,
                webConfig.getWidgets().get(("contractor_enrichment"))));
    }
}
