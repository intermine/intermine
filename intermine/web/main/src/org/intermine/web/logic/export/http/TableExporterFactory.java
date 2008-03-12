package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.TableExportConfig;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ExportException;


/**
 * Factory class that creates and registers TableHttpExporter classes.  
 * @author Jakub Kulaviak
 **/
public class TableExporterFactory
{
    
    private static final String TAB = "tab";
    private static final String CSV = "csv";
    private static final String EXCEL = "excel";
    
    private static Map<String, TableHttpExporter> exporters = 
        new HashMap<String, TableHttpExporter>();
        
    /**
     * Constructor.
     * @param request request that contains config with configured exporters
     */
    public TableExporterFactory(HttpServletRequest request) {
        register(TAB, new TabHttpExporter());
        register(CSV, new CSVHttpExporter());
        register(EXCEL, new ExcelHttpExporter());
        try {
            processConfig(request);    
        } catch (Exception e) {
            throw new ExportException("Export failed.", e);
        }
    }

    private void processConfig(HttpServletRequest request) throws Exception {
        WebConfig wc = (WebConfig) request.getSession().getServletContext().
            getAttribute(Constants.WEBCONFIG);
        Map<String, TableExportConfig> configs = wc.getTableExportConfigs();
        for (String key : configs.keySet()) {
            TableExportConfig config = configs.get(key);
            TableHttpExporter exporter = (TableHttpExporter) Class.
                forName(config.getClassName()).newInstance();
            exporters.put(key, exporter);
        }
    }

    /**
     * @param id id of required exporter 
     * @return exporter or null if exporter with given id doesn't exist
     */
    public TableHttpExporter getExporter(String id) {
        return exporters.get(id);
    }

    /**
     * Register exporter.   
     * @param id id of registered exporter
     * @param exporter exporter
     */
    public void register(String  id, TableHttpExporter exporter) {
        TableHttpExporter exp = getExporter(id);
        if (exp != null) {
            exporters.put(id, exporter);
        } 
    }
}