package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

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
    private static Map<String, String> exporters = new HashMap<String, String>();
    private Map<String, TableExportConfig> configs = new HashMap<String, TableExportConfig>();

    /**
     * Constructor.
     * @param webConfig web config with configured exporters
     */
    public TableExporterFactory(WebConfig webConfig) {
        try {
            register(TAB, TabHttpExporter.class.getCanonicalName());
            register(CSV, CSVHttpExporter.class.getCanonicalName());
            processConfig(webConfig);
        } catch (Exception e) {
            throw new ExportException("Export failed.", e);
        }
    }

    private void processConfig(WebConfig webConfig) throws Exception {
        configs = webConfig.getTableExportConfigs();
        for (String key : configs.keySet()) {
            TableExportConfig config = configs.get(key);
            register(key, config.getClassName());
        }
    }

    /**
     * Return the exporter class for the given id.
     * @param id id of required exporter
     * @return exporter or null if exporter with given id doesn't exist
     * @throws Exception if an error happens during obtaining of exporter
     */
    public TableHttpExporter getExporter(String id) throws Exception {
        String className = exporters.get(id);
        if (className != null) {
            return (TableHttpExporter) Class.forName(className).newInstance();
        } else {
            return null;
        }
    }

    /**
     * Return the TableExportConfig for the given id.
     * @param id id of required config
     * @return config or null if config with given id doesn't exist
     */
    public TableExportConfig getConfig(String id) {
        return configs.get(id);
    }

    /**
     * Register exporter.
     * @param id id of registered exporter
     * @param className class name of exporter
     * @throws Exception if an error happens during registering of exporter
     */
    public void register(String  id, String className) throws Exception {
        TableHttpExporter exp = getExporter(id);
        if (exp == null) {
            exporters.put(id, className);
        }
    }
}
