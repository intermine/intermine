package org.intermine.webservice.client.live;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.WidgetService;
import org.intermine.webservice.client.util.TestUtil;
import org.intermine.webservice.client.widget.Widget;
import org.junit.Test;

public class LiveWidgetsTest {

    private static final WidgetService authorised =
            new ServiceFactory(TestUtil.getRootUrl(), TestUtil.getToken()).getWidgetService();
    private static final WidgetService unauthorised =
            new ServiceFactory(TestUtil.getRootUrl()).getWidgetService();

    @Test
    public void getWidgets() {
        List<Widget> widgets = unauthorised.getWidgets();
        List<Widget> widgets2 = authorised.getWidgets();

        assertEquals(5, widgets.size());
        assertEquals(widgets.size(), widgets2.size());
    }

    @Test
    public void getWidget() {
        Widget widget = unauthorised.getWidget("full_part_time");
        assertEquals("Full-Time Status by Department", widget.getTitle());
        assertEquals("For each department associated with an item show the number of workers in that department in full or part-time employment", widget.getDescription());
        assertEquals("Department", widget.getXAxisLabel());
        assertEquals("Count", widget.getYAxisLabel());
    }

    @Test
    public void getChartWidgets() {
        List<Widget> chartWidgets = unauthorised.getChartWidgets();
        assertEquals(3, chartWidgets.size());
        Set<String> chartTypes = new HashSet<String>();
        chartTypes.addAll(Arrays.asList("BarChart", "StackedBarChart", "PieChart",
                "ScatterPlot", "XYLineChart", "ColumnChart"));
        for (Widget w: chartWidgets) {
            assertTrue(w.getChartType() + " is a known chart type.", chartTypes.contains(w.getChartType()));
        }
    }

    @Test
    public void getEnrichmentWidgets() {
        List<Widget> enrichments = unauthorised.getEnrichmentWidgets();
        assertEquals(2, enrichments.size());
        assertEquals("colleague_enrichment", enrichments.get(0).getName());
    }

    @Test
    public void getTargets() {
        Widget w = unauthorised.getWidget("full_part_time");
        assertEquals(1, w.getTargets().size());
        assertTrue(w.getTargets() + " is [Employee]", w.getTargets().containsAll(Arrays.asList("Employee")));
        for (Widget widget: unauthorised.getWidgets()) {
            assertTrue(widget.getTargets().size() > 0);
        }
    }

}
