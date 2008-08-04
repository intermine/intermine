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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.widget.config.GridWidgetConfig;


/**
 * @author "Dominik Grimm"
 *
 */
public class GridWidget extends Widget
{

    protected static final Logger LOG = Logger.getLogger(GridWidget.class);
    private int notAnalysed = 0;
    private InterMineBag bag;
    private ObjectStore os;
    private GridDataSetLdr dataSetLdr;
    private GridDataSet dataSet;
    private List<String[]> intersectionList = new Vector<String[]>();
    private List<String[]> intersectionListDown = new Vector<String[]>();
    private List<List<String[]>> flattenedResults = new Vector<List<String[]>>();
    private List<List<String[]>> flattenedResultsDown = new Vector<List<String[]>>();
    private String highlight;
    private String pValue, numberOpt;


    /**
     * @param config widget config
     * @param interMineBag bag for this widget
     * @param os object store
     * @param highlight for highlighting
     * @param selectedExtraAttribute ignore
     * @param pValue pValue
     * @param numberOpt as percentage or real number
     */
    public GridWidget(GridWidgetConfig config, InterMineBag interMineBag,
                            ObjectStore os, String selectedExtraAttribute,
                            String highlight, String pValue, String numberOpt) {
        super(config);
        this.bag = interMineBag;
        this.os = os;
        this.highlight = highlight;
        this.pValue = pValue;
        this.numberOpt = numberOpt;
        process();
    }

    /**
     * {@inheritDoc}
     */
    public void process() {
        try {
            String dataSetLoader = config.getDataSetLoader();
            Class<?> clazz = TypeUtil.instantiate(dataSetLoader);
            Constructor<?> constr;
            constr = clazz.getConstructor(new Class[]
                    {
                        InterMineBag.class, ObjectStore.class, String.class
                    });
            dataSetLdr = (GridDataSetLdr) constr.newInstance(new Object[]
                {
                    bag, os, pValue
                });
            notAnalysed = bag.getSize() - dataSetLdr.getWidgetTotal();
            
           dataSet = dataSetLdr.getGridDataSet();
                
           List<String> columns = dataSet.getSampleNames();
           List<String> columns2 = dataSet.getSampleNames();
                
           Map<String, Map<String, List<String>>> valueMapList = dataSet.getResults();
           boolean useDown = false;
           for (int i = 0; i < columns.size(); i++) {
                    Map<String, List<String>> valuesMap = valueMapList.get(columns.get(i));
                    intersectionList = new Vector<String[]>();
                    intersectionListDown = new Vector<String[]>();
                    for (int j = 0; j < columns2.size(); j++) {
                        int intersectionCount = 0;
                        int intersectionCountDown = 0;
                        int interBefor = intersectionList.size();
                        int interBeforDown = intersectionListDown.size(); 
                        Map<String, List<String>> values2Map = valueMapList.get(columns2.get(j));
                        List<String> tmpValues = null, tmpValues2 = null;
                        
                        if (valuesMap.get("UP") != null && values2Map.get("UP") != null) {
                            List<String> values = valuesMap.get("UP");
                            List<String> values2 = values2Map.get("UP");
                            if (values.size() <= values2.size()) {
                                tmpValues = values2;
                                tmpValues2 = values;
                            } else if (values.size() > values2.size()) {
                                tmpValues = values;
                                tmpValues2 = values2;
                            } else {
                                tmpValues = values2;
                                tmpValues2 = values;
                            }
                            for (int k = 0; k < tmpValues.size(); k++) {
                                if (tmpValues2.contains(tmpValues.get(k))) {
                                    intersectionCount++;
                                }
                            }
                            //create link, highlighting and store values
                            String[] tmp = new String[5];
                            
                            double p = calcRValue(values.size(),
                                                  values2.size(),
                                                  intersectionCount,
                                                  bag.size());
                            
                            if (numberOpt.equals("number")) {
                                tmp[0] = String.valueOf(intersectionCount);
                            } else {
                                String pR = String.valueOf(p * 100);
                                if (pR.length() > 4) {
                                    if  (pR.equals("100.0")) {
                                        pR = "100";
                                    } else {
                                        pR = pR.substring(0, 4);
                                    }
                                }
                                tmp[0] = pR + "%";
                            }
                            tmp[1] = "widgetAction.do?bagName=" + bag.getName() 
                            + "&link=" + config.getLink()
                            + "&key=" + URLEncoder.encode(columns.get(i) 
                                    + "_" + columns2.get(j), "UTF-8");
                            tmp[2] = calcRGB(p);
                            if (numberOpt.equals("number")) {
                                tmp[3] = String.valueOf(p * 100) + "%";
                            } else {
                                tmp[3] = String.valueOf(intersectionCount) + " intersections";
                            }
                            tmp[4] = "UP";
                            
                            intersectionList.add(tmp);
                        } 
                        if (valuesMap.get("DOWN") != null && values2Map.get("DOWN") != null) {
                            useDown = true;
                            List<String> values = valuesMap.get("DOWN");
                            List<String> values2 = values2Map.get("DOWN");
                            if (values.size() <= values2.size()) {
                                tmpValues = values2;
                                tmpValues2 = values;
                            } else if (values.size() > values2.size()) {
                                tmpValues = values;
                                tmpValues2 = values2;
                            } else {
                                tmpValues = values2;
                                tmpValues2 = values;
                            }
                            for (int k = 0; k < tmpValues.size(); k++) {
                                if (tmpValues2.contains(tmpValues.get(k))) {
                                    intersectionCountDown++;
                                }
                            }
                            //create link, highlighting and store values
                            String[] tmp = new String[5];
                            double p = calcRValue(values.size(),
                                                  values2.size(),
                                                  intersectionCountDown,
                                                  bag.size());
                            
                            if (numberOpt.equals("number")) {
                                tmp[0] = String.valueOf(intersectionCountDown);
                            } else {
                                String pR = String.valueOf(p * 100);
                                if (pR.length() > 4) {
                                    if  (pR.equals("100.0")) {
                                        pR = "100";
                                    } else {
                                        pR = pR.substring(0, 4);
                                    }
                                }
                                tmp[0] = pR + "%";
                            }
                            tmp[1] = "widgetAction.do?bagName=" + bag.getName() 
                            + "&link=" + config.getLink()
                            + "&key=" + URLEncoder.encode(columns.get(i) 
                                    + "_" + columns2.get(j), "UTF-8");
                            tmp[2] = calcRGB(p);
                            if (numberOpt.equals("number")) {
                                tmp[3] = String.valueOf(p * 100) + "%";
                            } else {
                                tmp[3] = String.valueOf(intersectionCountDown) + " intersections";
                            }
                            tmp[4] = "DOWN";
                            intersectionListDown.add(tmp);
                        }
                        if (interBefor == intersectionList.size()) {
                            String[] tmp = new String[5];
                            if (numberOpt.equals("number")) {
                                tmp[0] = "0";
                            } else {
                                tmp[0] = "0%";
                            }
                            tmp[1] = "widgetAction.do?bagName=" + bag.getName() 
                            + "&link=" + config.getLink()
                            + "&key=" + URLEncoder.encode(columns.get(i) 
                                    + "_" + columns2.get(j), "UTF-8");
                            tmp[2] = "00FF50";
                            if (numberOpt.equals("number")) {
                                tmp[3] = "0%";
                            } else {
                                tmp[3] = "0 intersections";
                            }
                            tmp[4] = "UP";
                            intersectionList.add(tmp);
                        }
                        if (interBeforDown == intersectionListDown.size()) {
                            String[] tmp = new String[5];
                            if (numberOpt.equals("number")) {
                                tmp[0] = "0";
                            } else {
                                tmp[0] = "0%";
                            }
                            tmp[1] = "widgetAction.do?bagName=" + bag.getName() 
                            + "&link=" + config.getLink()
                            + "&key=" + URLEncoder.encode(columns.get(i) 
                                    + "_" + columns2.get(j), "UTF-8");
                            tmp[2] = "00FF50";
                            if (numberOpt.equals("number")) {
                                tmp[3] = "0%";
                            } else {
                                tmp[3] = "0 intersections";
                            }
                            tmp[4] = "DOWN";
                            intersectionListDown.add(tmp);
                        }
                    }
                    flattenedResults.add(intersectionList);
                    if (useDown) {
                        flattenedResultsDown.add(intersectionListDown);
                    } else {
                        intersectionListDown = new Vector<String[]>();
                        flattenedResultsDown.add(intersectionListDown);
                    }
                }
            
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
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
        } catch (ObjectStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private double calcRValue(int values1Size,
                              int values2Size,
                              int intersectionCount,
                              int bagSize) {
      //probability: p
        //valuesA: x
        //valuesB: y
        //intersectionCount: z
        // p = 1 - ((x + y) - 2 * z) / (x + y)
        
        double termA = ((values1Size + values2Size)
                - 2 * intersectionCount);
        double termB = values1Size + values2Size;
        
        double p = 0;
        
        if (highlight.equals("cell type to cell type")) {
            p = 1 - (termA / termB);
        }
        else if (highlight.equals("cell type in the list")) {
            p = ((double) intersectionCount) / ((double) bagSize);
        }
        return p;
    }
    
    private String calcRGB(double p) {
      //color spectrum calculation
        
        int red = 0, blue = 0, green = 0;
        
        if (p >= 0.7) {
            red = 255;
            blue = 0;
            green = 200 - (int) (90 * (((p - 0.7) * 100) / 30.0));
        } else if (p >= 0.5 && p < 0.7) {
            red = 255;
            blue = 0;
            green = 255 - (int) (55 * (((p - 0.5) * 100) / 20.0));
        } else if (p > 0.3 && p < 0.5) {
            blue = 0;
            green = 255;
            red = 180 + (int) (75 * (((p - 0.3) * 100) / 20.0));
        } else {
            blue = 0;
            green = 255;
            red =  80 + (int) (100 * (((p) * 100) / 30.0));
        }
        
        //convert to hex
        String redHex = Integer.toHexString(red);
        String greenHex = Integer.toHexString(green);
        String blueHex = Integer.toHexString(blue);
        
        if (redHex.length() == 1) {
            redHex = "0" + redHex;
        }
        if (greenHex.length() == 1) {
            greenHex = "0" + greenHex;
        }
        if (blueHex.length() == 1) {
            blueHex = "0" + blueHex;
        }
        
        //build RGB color code from hex
        return redHex + greenHex + blueHex;   
    }
    
    /**
     * {@inheritDoc}
     */
    public List getElementInList() {
        return flattenedResultsDown;
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
        return (dataSet.getResults().size() > 0);
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String>> getExportResults(String[]selected) throws Exception {

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List getFlattenedResults() {
        
        return flattenedResults;
    }

    /**
    *
    * @return List of column labels
    */
   public List<String> getColumns() {
       return dataSet.getSampleNames();
   }

}
