package org.intermine.webservice.client.live;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.WidgetService;
import org.intermine.webservice.client.widget.Widget;
import org.junit.Test;

public class LiveWidgetsTest {

    private static final String baseUrl = "http://localhost/intermine-test/service";
    private static final String authToken = "test-user-token";
    private static final WidgetService authorised = new ServiceFactory(baseUrl, authToken).getWidgetService();
    private static final WidgetService unauthorised = new ServiceFactory(baseUrl).getWidgetService();

    @Test
    public void getWidgets() {
        List<Widget> widgets = unauthorised.getWidgets();
        List<Widget> widgets2 = authorised.getWidgets();

        assertEquals(6, widgets.size());
        assertEquals(widgets.size(), widgets2.size());
    }

    @Test
    public void getWidget() {
        Widget widget = unauthorised.getWidget("age_salary");
        assertEquals("Age - Salary trend line", widget.getTitle());
        assertEquals("Relationship between age and salaries for CEOs", widget.getDescription());
        assertEquals("Age", widget.getXAxisLabel());
        assertEquals("Salary", widget.getYAxisLabel());
    }

    @Test
    public void getChartWidgets() {
        List<Widget> chartWidgets = unauthorised.getChartWidgets();
        assertEquals(5, chartWidgets.size());
        Set<String> chartTypes = new HashSet<String>();
        chartTypes.addAll(Arrays.asList("BarChart", "StackedBarChart", "PieChart",
                "ScatterPlot", "XYLineChart"));
        for (Widget w: chartWidgets) {
            assertTrue(chartTypes.contains(w.getChartType()));
        }
    }

    @Test
    public void getEnrichmentWidgets() {
        List<Widget> enrichments = unauthorised.getEnrichmentWidgets();
        assertEquals(1, enrichments.size());
        assertEquals("contractor_enrichment", enrichments.get(0).getName());
    }

    @Test
    public void getTargets() {
        Widget w = unauthorised.getWidget("full_part_time");
        assertEquals(2, w.getTargets().size());
        assertTrue(w.getTargets().containsAll(Arrays.asList("CEO", "Company")));
        for (Widget widget: unauthorised.getWidgets()) {
            assertTrue(widget.getTargets().size() > 0);
        }
    }

}
