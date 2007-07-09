package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.BagTableDisplayer;
import org.intermine.web.logic.config.GraphDisplayer;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.widget.BagGraphWidget;
import org.intermine.web.logic.widget.BagTableWidgetLoader;
import org.intermine.web.logic.widget.DataSetLdr;
import org.intermine.web.logic.widget.GraphDataSet;

import java.awt.Font;

import java.lang.reflect.Constructor;

import javax.servlet.ServletContext;
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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.urls.CategoryURLGenerator;

/**
 * @author Xavier Watkins
 */
public class BagDetailsController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context, 
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
                    throws Exception {

            HttpSession session = request.getSession();
            ServletContext servletContext = session.getServletContext();
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

            String bagName = request.getParameter("bagName");
            if (bagName == null) {
                bagName = request.getParameter("name");
            }
            
            InterMineBag imBag;
            if (request.getParameter("scope") != null 
                            && request.getParameter("scope").equals("global")) {
                SearchRepository searchRepository =
                    SearchRepository.getGlobalSearchRepository(servletContext);
                Map<String, ? extends WebSearchable> publicBagMap = 
                    searchRepository.getWebSearchableMap(TagTypes.BAG);
                imBag = (InterMineBag) publicBagMap.get(bagName); 
                
            } else {
                Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
                imBag = profile.getSavedBags().get(bagName);
            }
            
            /* forward to bag page if this is an invalid bag */
            if (imBag == null) {
                return null; 
            }

            Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
            WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
            Model model = os.getModel();
            Type type = (Type) webConfig.getTypes().get(model.getPackageName() 
                                                        + "." + imBag.getType());

            Set graphDisplayers = type.getGraphDisplayers();
            ArrayList<String[]> graphDisplayerArray = new ArrayList<String[]>();
            for (Iterator iter = graphDisplayers.iterator(); iter.hasNext();) {

                try {

                    GraphDisplayer graphDisplayer = (GraphDisplayer) iter.next();
                    String dataSetLoader = graphDisplayer.getDataSetLoader();
                    Class clazz = TypeUtil.instantiate(dataSetLoader);
                    Constructor constr = clazz.getConstructor(new Class[]
                                                                        {
                        InterMineBag.class, ObjectStore.class
                                                                        });

                    DataSetLdr dataSetLdr = (DataSetLdr) constr.newInstance(new Object[]
                                                                                       {
                        imBag, os
                                                                                       });

                    //TODO use caching here
                    if (!dataSetLdr.getDataSets().isEmpty()) {
                        for (Iterator it 
                                  = dataSetLdr.getDataSets().keySet().iterator(); it.hasNext();) {
                            String key = (String) it.next();            
                            GraphDataSet graphDataSet 
                            = (GraphDataSet) dataSetLdr.getDataSets().get(key);
                            /* stacked bar chart */
                            if (graphDisplayer.getGraphType().equals("StackedBarChart")) {
                                setStackedBarGraph(session, graphDisplayer, graphDataSet, 
                                                   graphDisplayerArray, bagName);
                            /* regular bar chart */
                            } else {
                                setBarGraph(session, graphDisplayer, graphDataSet, 
                                            graphDisplayerArray, bagName, key);
                            } 
                        }
                    }


                } catch  (Exception e) {
                    // TODO do something clever
                    //return null;
                    //throw new Exception();
                }
            }

            ArrayList<BagTableWidgetLoader> tableDisplayerArray 
                                                           = new ArrayList<BagTableWidgetLoader>();
            Set bagTabledisplayers = type.getBagTableDisplayers();
            for (Iterator iter = bagTabledisplayers.iterator(); iter.hasNext();) {
                try {
                    BagTableDisplayer bagTableDisplayer = (BagTableDisplayer) iter.next();
                    String ldrType = bagTableDisplayer.getType();
                    String collectionName = bagTableDisplayer.getCollectionName();
                    String fields = bagTableDisplayer.getFields();
                    String title = bagTableDisplayer.getTitle();
                    String description = bagTableDisplayer.getDescription();
                    BagTableWidgetLoader bagWidgLdr =
                        new BagTableWidgetLoader(title, description, ldrType, collectionName,
                                                 imBag, os, webConfig, model,
                                                 classKeys, fields);
                    tableDisplayerArray.add(bagWidgLdr);

                } catch  (Exception e) {
                    // TODO do something clever
                    //return null;
                    //throw new Exception();
                }
            }

            Query q = new Query();
            QueryClass qc = new QueryClass(InterMineObject.class);
            q.addFrom(qc);
            q.addToSelect(qc);
            q.setConstraint(new BagConstraint(qc, ConstraintOp.IN, imBag.getOsb()));
            q.setDistinct(false);
            SingletonResults res = os.executeSingleton(q);

            WebPathCollection webPathCollection =
                new WebPathCollection(os, new Path(model, imBag.getType()), res, model, webConfig,
                                      classKeys);

            PagedTable pagedColl = new PagedTable(webPathCollection);

            request.setAttribute("bag", imBag);
            request.setAttribute("bagSize", new Integer(imBag.size()));
            request.setAttribute("pagedColl", pagedColl);
            request.setAttribute("graphDisplayerArray", graphDisplayerArray);
            request.setAttribute("tableDisplayerArray", tableDisplayerArray);
            return null;



    }


    private void setBarGraph(HttpSession session, 
                             GraphDisplayer graphDisplayer, 
                             GraphDataSet graphDataSet,                          
                             ArrayList<String[]> graphDisplayerArray,
                             String bagName,
                             String subtitle) {
        JFreeChart chart = null;
        CategoryPlot plot = null;
        BagGraphWidget bagGraphWidget = null;

        chart = ChartFactory.createBarChart(
                graphDisplayer.getTitle(),          // chart title
                graphDisplayer.getDomainLabel(),    // domain axis label
                graphDisplayer.getRangeLabel(),     // range axis label
                graphDataSet.getDataSet(),            // data 
                PlotOrientation.VERTICAL, 
                true, 
                true,                               // tooltips? 
                false                               // URLs? 
        );    
       
        TextTitle subtitleText = new TextTitle(subtitle);
        subtitleText.setFont(new Font("SansSerif", Font.ITALIC, 10));
        chart.addSubtitle(subtitleText);
                
        plot = chart.getCategoryPlot();

        BarRenderer renderer = new BarRenderer();
        renderer.setItemLabelsVisible(true);
        renderer.setItemMargin(0);
        plot.setRenderer(renderer);
        CategoryURLGenerator categoryUrlGen = null;
        // set series 0 to have URLgenerator specified in config file
        // set series 1 to have no URL generator.
        try {
            Class clazz2 = TypeUtil.instantiate(graphDisplayer.getUrlGen());                    
            Constructor urlGenConstructor = clazz2.getConstructor(new Class[]
                                                                            {
                String.class
                                                                            });
            categoryUrlGen = (CategoryURLGenerator) urlGenConstructor
            .newInstance(new Object[]
                                    {
                bagName
                                    });
            
        } catch (Exception err) {
            err.printStackTrace();
        }
        renderer.setItemURLGenerator(null);    
        renderer.setSeriesItemURLGenerator(0, categoryUrlGen);
        renderer.setSeriesItemURLGenerator(1, null);
        
        // integers only
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
                       
        bagGraphWidget = new BagGraphWidget(session, 
                         graphDataSet.getCategoryArray(),
                         bagName,
                         graphDisplayer.getToolTipGen(),
                         null,
                         chart,
                         plot,
                         renderer);

        graphDisplayerArray.add(new String[] {
            bagGraphWidget.getHTML(), graphDisplayer.getDescription()
        });        
    }
    
    private void setStackedBarGraph(HttpSession session, 
                                    GraphDisplayer graphDisplayer, 
                                    GraphDataSet graphDataSet,                                  
                                    ArrayList<String[]> graphDisplayerArray,
                                    String bagName) {
        
        JFreeChart chart = null;
        CategoryPlot plot = null;
        BagGraphWidget bagGraphWidget = null;
        
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
        bagGraphWidget = new BagGraphWidget(session,
                         graphDataSet.getCategoryArray(),
                         bagName, 
                         graphDisplayer.getToolTipGen(),
                         graphDisplayer.getUrlGen(), 
                         chart,
                         plot,
                         renderer);
        
        graphDisplayerArray.add(new String[] {
            bagGraphWidget.getHTML(), graphDisplayer.getDescription()
        });  
    }
}

