package org.flymine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.GraphDisplayer;
import org.intermine.web.logic.widget.BagGraphWidget;
import org.intermine.web.logic.widget.DataSetLdr;
import org.intermine.web.logic.widget.GraphDataSet;

import org.flymine.web.widget.FlyFishDataSetLdr;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;

/**
 * Controller for flyFishDisplayer.jsp
 * @author Julie Sullivan
 */
public class FlyFishDisplayerController extends TilesAction
{
    /**
     * @see TilesAction#execute(ComponentContext, ActionMapping, ActionForm, HttpServletRequest,
     *                          HttpServletResponse)
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        try {
            HttpSession session = request.getSession();
            ObjectStore os =
                (ObjectStore) session.getServletContext().getAttribute(Constants.OBJECTSTORE);
            String identifier = (String) request.getAttribute("id");

            DataSetLdr dataSetLdr = new FlyFishDataSetLdr(identifier, os);
            if (!dataSetLdr.getDataSets().isEmpty()) {
                for (Iterator it
                          = dataSetLdr.getDataSets().keySet().iterator(); it.hasNext();) {
                    String key = (String) it.next();
                    GraphDataSet graphDataSet = (GraphDataSet) dataSetLdr.getDataSets().get(key);
                    GraphDisplayer graphDisplayer = new GraphDisplayer();
                    String html = setStackedBarGraph(session, graphDisplayer, graphDataSet,
                                                     identifier);
                    request.setAttribute("html", html);
                }
            }

        } catch (Exception err) {
            err.printStackTrace();
        }

        return null;
    }

    private String setStackedBarGraph(HttpSession session,
                                     GraphDisplayer graphDisplayer,
                                     GraphDataSet graphDataSet,
                                     String geneName) {

         JFreeChart chart = null;
         CategoryPlot plot = null;

         chart = ChartFactory.createStackedBarChart(
                 graphDisplayer.getTitle(),       // chart title
                 graphDisplayer.getDomainLabel(), // domain axis label
                 graphDisplayer.getRangeLabel(),  // range axis label
                 graphDataSet.getDataSet(),         // data
                 PlotOrientation.VERTICAL,
                 true,
                 true,                            // tooltips?
                 false                            // URLs?
         );
         plot = chart.getCategoryPlot();
         StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
         BagGraphWidget bagGraphWidget = new BagGraphWidget(session,
                          graphDataSet.getCategoryArray(),
                          geneName,
                          graphDisplayer.getToolTipGen(),
                          graphDisplayer.getUrlGen(),
                          chart,
                          plot,
                          renderer);
         return bagGraphWidget.getHTML();
     }
}