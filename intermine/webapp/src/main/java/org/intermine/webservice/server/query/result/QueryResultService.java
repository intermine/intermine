package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.BagNotFound;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.ColumnHeaderStyle;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.core.CountProcessor;
import org.intermine.webservice.server.core.ResultProcessor;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.FilteringResultIterator;
import org.intermine.webservice.server.output.FlatFileFormatter;
import org.intermine.webservice.server.output.HTMLTableFormatter;
import org.intermine.webservice.server.output.JSONCountFormatter;
import org.intermine.webservice.server.output.JSONObjResultProcessor;
import org.intermine.webservice.server.output.JSONResultFormatter;
import org.intermine.webservice.server.output.JSONRowResultProcessor;
import org.intermine.webservice.server.output.JSONSummaryProcessor;
import org.intermine.webservice.server.output.JSONTableFormatter;
import org.intermine.webservice.server.output.JSONTableResultProcessor;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.query.AbstractQueryService;

/**
 * Executes query and returns results. Other parameters in request can specify
 * range of returned results, format ... For using of web service and parameter
 * description see InterMine wiki pages. 1) Validates parameters and tries
 * validate xml query as much as possible. Validates xml query according to XML
 * Schema and and finds out if there were some errors during unmarshalling
 * PathQuery from xml. 2) Executes created PathQuery. 3) Print results to
 * output.
 *
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
 */

public class QueryResultService extends AbstractQueryService
{

    /** Batch size to use **/
    public static final int BATCH_SIZE = 5000;
    protected Map<String, Object> attributes = new HashMap<String, Object>();
    protected LinkedHashMap<String, Object> dataPackageAttributes = new LinkedHashMap<String, Object>();

    private boolean wantsCount = false;
    private PathQueryExecutor executor;

    /**
     * Constructor
     * @param im The InterMineAPI settings bundle for this webservice
     */
    public QueryResultService(InterMineAPI im) {
        super(im);
    }

    /**
     * Executes service specific logic.
     */
    @Override
    protected void execute() {
        QueryResultInput input = getInput();
        PathQueryBuilder builder = getQueryBuilder(input.getXml());
        PathQuery query = builder.getQuery();
        setHeaderAttributes(query, input.getStart(), input.getLimit());
        runPathQuery(query, input.getStart(), input.getLimit());

        // will be replaced by isExportingDataPackage()
        if(!isUncompressed()) {
            exportDataPackage(query);
        }
    }

    @Override
    protected void initState() {
        super.initState();
        wantsCount = WebServiceRequestParser.isCountRequest(request);
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.TSV;
    }

    private static final Set<Format> MENU = new HashSet<Format>() {
        private static final long serialVersionUID = -6257564064566791521L;
        {
            addAll(Format.BASIC_FORMATS);
            addAll(Format.FLAT_FILES);
            addAll(Format.JSON_FORMATS);
        }
    };

    @Override
    protected boolean canServe(Format format) {
        return MENU.contains(format);
    }

    @Override
    protected void postInit() {
        executor = getPathQueryExecutor();
    }

    protected void exportDataPackage(PathQuery pq) {
        /*
        The structure of Data package is as follows -
        {
            ... (some attributes and values)
            resources : [
                {
                    ... (some attributes and values)
                    schema: {
                        fields: [
                            {column 1 details},
                            {column 2 details}, and so on...
                        ],
                        primaryKey: ["key1","key2","key3"]
                    }
                }
            ]
            sources: [
                {source 1 details},
                {source 2 details}, and so on...
            ]
        }

        only sources array (of objects) is hadcoded right now (work in progress)
        */

        ArrayList<Map<String, Object>> fields = new ArrayList<>();  // array of objects (column details)
        
        String clsName; // the name of root class
        try {
            clsName = pq.getRootClass();
        } catch (PathException e1) {
            // Check this
            throw new ServiceException(e1);
        }

        for (String v : pq.getView()) {
            try {
                // the column details object
                LinkedHashMap<String, Object> columnDetails = new LinkedHashMap<String, Object>();

                Path p = pq.makePath(v);

                // get type of column attribute
                AttributeDescriptor ad = (AttributeDescriptor) p.getEndFieldDescriptor();
                String type = ad.getType();
                int lastIndexOfDot = type.lastIndexOf('.');
                type = type.substring(lastIndexOfDot + 1);

                // get friendly path of column attribute
                String friendlyPath = WebUtil.formatPathDescription(v, pq, InterMineContext.getWebConfig());

                // make the column details object
                columnDetails.put("name", p.getLastElement());
                columnDetails.put("type", type);
                columnDetails.put("class path", friendlyPath);
                columnDetails.put("class ontology link", p.getLastClassDescriptor().getFairTerm());
                columnDetails.put("attribute ontology link", ((AttributeDescriptor) p.getEndFieldDescriptor()).getFairTerm());
                
                // add the column details object in fields array
                fields.add(columnDetails);
            } catch (PathException e) {
                throw new ServiceException(e);
            }
        }

        // make the schema object
        LinkedHashMap<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("fields", fields);
        schema.put("primaryKey", getPrimaryKeys(pq, clsName));

        // get format of results file
        String format = getFormatType();

        // get web service url for this query
        String xml = getQueryXML(null, pq);
        String serviceFormat;
        if (request.getParameter("serviceFormat") != null) {
            serviceFormat = request.getParameter("serviceFormat");
        } else {
            serviceFormat = "tab";
        }
        String link = new QueryResultLinkGenerator().getLink(new URLGenerator(request).getPermanentBaseURL(), xml,
                serviceFormat);

        // make the resource object of resources array
        LinkedHashMap<String, Object> resource = new LinkedHashMap<String, Object>();
        resource.put("profile", "tabular-data-resource");
        resource.put("name", "intermine-query-data-resource");
        resource.put("path", link);
        resource.put("format", format);
        resource.put("schema", schema);

        // the resources array always contains only 1 resource in our case
        ArrayList<Object> resources = new ArrayList<Object>();
        resources.add(resource);

        // hardcoded datasources for sample datapackage - WIP
        ArrayList<Object> dataSources = new ArrayList<Object>();
        LinkedHashMap<String, String> tempDataSource = new LinkedHashMap<String, String>();
        tempDataSource.put("title", "UniProt");
        tempDataSource.put("url", "UniProt URL");
        dataSources.add(tempDataSource);

        // finally, prepare the data package object to be exported
        dataPackageAttributes.put("profile", "data-package");
        dataPackageAttributes.put("name", "intermine-query");
        dataPackageAttributes.put("description", "A test InterMine query!");
        dataPackageAttributes.put("resources", resources);
        dataPackageAttributes.put("sources", tempDataSource);

        // write the dataPackageAttributes in a new zipFileEntry named datapackage.json
        try {
            // close the results file
            ((StreamedOutput) output).writeFooter();
            out.flush();

            // initialize the dataPackageOutput
            dataPackageOutput = makeJSONOutput(out, getLineBreak());
            ((ZipOutputStream) os).putNextEntry(new ZipEntry("datapackage.json"));

            // ObjectMapper for proper formatting
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // write in dataPackageOutput
            ((StreamedOutput) dataPackageOutput).writeLn(mapper.writeValueAsString(dataPackageAttributes));
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Set the header attributes of the output based on the values of the PathQuery
     *
     * @param pq The path query to be run
     * @param start The beginning of this set of results
     * @param size The size of this set of results
     */
    protected void setHeaderAttributes(PathQuery pq, Integer start, Integer size) {

        if (formatIsJSON()) {
            // These attributes are always needed
            attributes.put(JSONResultFormatter.KEY_MODEL_NAME, pq.getModel().getName());
            attributes.put(JSONResultFormatter.KEY_VIEWS, pq.getView());

            attributes.put(JSONTableFormatter.KEY_COLUMN_HEADERS,
                    WebUtil.formatPathQueryView(pq, InterMineContext.getWebConfig()));
            attributes.put("start", String.valueOf(start));
            try {
                attributes.put(JSONResultFormatter.KEY_ROOT_CLASS, pq.getRootClass());
            } catch (PathException e) {
                throw new ServiceException(e);
            }
            String summaryPath = request.getParameter("summaryPath");
            if (!isBlank(summaryPath)) {
                int count;
                try {
                    count = executor.uniqueColumnValues(pq, summaryPath);
                } catch (BagNotFound e) {
                    throw new BadRequestException(e.getMessage());
                } catch (ObjectStoreException e) {
                    throw new ServiceException("Problem getting unique column value count.", e);
                }
                attributes.put("uniqueValues", count);
            }
            if (formatIsJSONP()) {
                String callback = StringUtils.defaultString(getCallback(), "makeResultsTable");
                attributes.put(JSONResultFormatter.KEY_CALLBACK, callback);
            }
        } else if (formatIsFlatFile()) {
            if (wantsColumnHeaders()) {
                if (ColumnHeaderStyle.FRIENDLY == getColumnHeaderStyle()) {
                    attributes.put(FlatFileFormatter.COLUMN_HEADERS,
                            WebUtil.formatPathQueryView(pq, InterMineContext.getWebConfig()));
                } else {
                    attributes.put(FlatFileFormatter.COLUMN_HEADERS, pq.getView());
                }
            }
        }

        switch(getFormat()) {
            case TABLE:
                List<String> viewTypes = new ArrayList<String>();
                for (String v: pq.getView()) {
                    try {
                        Path p = pq.makePath(v);
                        AttributeDescriptor ad = (AttributeDescriptor) p.getEndFieldDescriptor();
                        viewTypes.add(ad.getType());
                    } catch (PathException e) {
                        throw new ServiceException(e);
                    }
                }
                String title = pq.getTitle();
                String description = StringUtils.defaultString(pq.getDescription(), pq.toString());
                attributes.put("viewTypes", viewTypes);
                attributes.put("size", String.valueOf(size));
                attributes.put(JSONTableFormatter.KEY_TITLE, title);
                attributes.put(JSONTableFormatter.KEY_DESCRIPTION, description);
                break;
            case HTML:
                attributes.put(HTMLTableFormatter.KEY_COLUMN_HEADERS,
                        WebUtil.formatPathQueryView(pq, InterMineContext.getWebConfig()));
                break;
            default:
                break;
        }

        if (!wantsCount) { // mutually exclusive options.
            String summaryPath = getOptionalParameter("summaryPath");
            if (isNotBlank(summaryPath)) {
                Path p;
                try {
                    p = pq.makePath(summaryPath);
                } catch (PathException e) {
                    throw new BadRequestException("Summary path is invalid");
                }
                if (!p.endIsAttribute()) {
                    throw new BadRequestException("Summary path is invalid");
                }
                AttributeDescriptor ad = (AttributeDescriptor) p.getEndFieldDescriptor();
                String type = ad.getType();
                List<String> colHeaders = new ArrayList<String>();
                if ("int".equals(type) || "Integer".equals(type) || "Float".equals(type)
                        || "float".equals(type) || "Double".equals(type)
                        || "double".equals(type) || "long".equals(type)
                        || "Long".equals(type) || "Math.BigDecimal".equals(type)) {
                    colHeaders.addAll(Arrays.asList("min", "max", "average", "standard-dev"));
                } else {
                    colHeaders.addAll(Arrays.asList("item", "count"));
                }

                if (formatIsJSON()) {
                    attributes.put(JSONTableFormatter.KEY_COLUMN_HEADERS, colHeaders);
                } else if (formatIsFlatFile() && wantsColumnHeaders()) {
                    attributes.put(FlatFileFormatter.COLUMN_HEADERS, colHeaders);
                }
            }
        }

        output.setHeaderAttributes(attributes);
    }

    @Override
    protected Output makeJSONOutput(PrintWriter out, String separator) {
        if (wantsCount) {
            return new StreamedOutput(out, new JSONCountFormatter(), separator);
        }
        return new StreamedOutput(out, new JSONTableFormatter(), separator);
    }

    /**
     * URL Encode an object. Null values are returned as the empty string, and encoding problems
     * throw runtime exceptions.
     * @param o The thing to encode.
     * @return The encoded version.
     */
    protected static String encode(Object o) {
        if (o == null) {
            return "";
        } else {
            try {
                return URLEncoder.encode(o.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding string failed: "
                        + o.toString(), e);
            }
        }
    }

    private void runCount(PathQuery pathquery) {
        int count;
        try {
            count = executor.count(pathquery);
        } catch (ObjectStoreException e) {
            throw new ServiceException("Problem getting count.", e);
        }
        CountProcessor processor = new CountProcessor();
        processor.writeCount(count, output);
    }

    /**
     * Runs path query and returns to output obtained results.
     *
     * @param pathQuery
     *            path query
     * @param firstResult
     *            index of first result, that should be returned
     * @param maxResults
     *            maximum number of results
     */
    public void runPathQuery(PathQuery pathQuery, int firstResult, int maxResults) {
        if (wantsCount) {
            runCount(pathQuery);
        } else {
            runResults(pathQuery, firstResult, maxResults);
        }
    }

    private void runResults(PathQuery pq, int firstResult, int maxResults) {
        final boolean canGoFaster;
        final Iterator<List<ResultElement>> it;
        final String summaryPath = getOptionalParameter("summaryPath");
        if (isNotBlank(summaryPath)) {
            Integer uniqs = (Integer) attributes.get("uniqueValues");
            boolean occurancesOnly = (uniqs == null) || (uniqs < 2);
            try {
                String filterTerm = getOptionalParameter("filterTerm");
                Results r = executor.summariseQuery(pq, summaryPath, filterTerm, occurancesOnly);
                try {
                    // causes query to be strictly evaluated, and errors to surface here.
                    r.range(0, 0);
                } catch (IndexOutOfBoundsException e) {
                    // Ignore, it just means it's empty.
                }
                if (filterTerm != null) {
                    attributes.put("filteredCount", r.size());
                }
                it = new FilteringResultIterator(r, firstResult, maxResults, filterTerm);
                canGoFaster = false;
            } catch (ObjectStoreQueryDurationException e) {
                throw new ServiceException("Query would take too long to run");
            } catch (ObjectStoreException e) {
                throw new ServiceException("Problem getting summary.", e);
            }
        } else {
            canGoFaster = maxResults > (BATCH_SIZE * 2);
            executor.setBatchSize(BATCH_SIZE);
            try {
                it = executor.execute(pq, firstResult, maxResults);
            } catch (ObjectStoreQueryDurationException e) {
                throw new ServiceException("Query would take too long to run");
            } catch (ObjectStoreException e) {
                throw new ServiceException("Problem getting summary.", e);
            }
        }

        ResultProcessor processor = makeResultProcessor();
        if (it.hasNext()) { // Prime the batch fetching pumps
            try {
                if (canGoFaster) {
                    // Going faster means writing to the DB. Don't do this if it is pointless.
                    ((ExportResultsIterator) it).goFaster();
                }
                processor.write(it, output);
            } finally {
                if (canGoFaster) {
                    ((ExportResultsIterator) it).releaseGoFaster();
                }
            }
        }
    }

    private ResultProcessor makeResultProcessor() {
        ResultProcessor processor;
        boolean summarising = StringUtils.isNotBlank(request.getParameter("summaryPath"));
        switch(getFormat()) {
            case JSON:
                processor
                    = new JSONRowResultProcessor(im, JSONRowResultProcessor.Verbosity.MINIMAL);
                break;
            case OBJECTS:
                processor = new JSONObjResultProcessor();
                break;
            case TABLE:
                processor = new JSONTableResultProcessor();
                break;
            case ROWS:
                if (summarising) {
                    processor = new JSONSummaryProcessor();
                } else {
                    processor = new JSONRowResultProcessor(im);
                }
                break;
            default:
                processor = new ResultProcessor();
        }
        return processor;
    }
    /**
     * Return the PathQueryExecutor
     *
     * @return the PathQueryExecutor
     */
    protected PathQueryExecutor getPathQueryExecutor() {
        final Profile profile = getPermission().getProfile();
        return im.getPathQueryExecutor(profile);
    }


    private QueryResultInput getInput() {
        QueryResultInput qri = new QueryResultRequestParser(im.getQueryStore(),
                request).getInput();
        // Table format doesn't actually fetch any rows but we want it to trigger a query in
        // ObjectStore so results are in cache when Row processors need to fetch them. We need
        // to set a limit here to prevent runResults() from calling goFaster() and precomputing.
        if (getFormat() == Format.TABLE) {
            qri.setLimit(WebServiceRequestParser.MIN_LIMIT);
        }
        return qri;
    }

    /**
     * loads the class_keys.properties file to return primary keys of a class
     *
     * @param pq path query
     * @param clsName   name of class for which keys will looked up
     * @return  primary keys for the class
     *              
     */
    private List<String> getPrimaryKeys(PathQuery pq, String clsName) {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("class_keys.properties"));
        } catch (IOException e1) {
            // Fix me
            throw new ServiceException(e1);
        }
        Map<String, List<FieldDescriptor>> classKeys = ClassKeyHelper.readKeys(pq.getModel(), props);
        List<String> keys = ClassKeyHelper.getKeyFieldNames(classKeys, clsName);
        return keys;
    }

    /**
     * @return the format requested by user for results file
     */
    private String getFormatType() {
        String format;
        switch (getFormat()) {
            case HTML:
                format = "html"; 
                break;
            case XML:
                format = "xml";
                break;
            case TSV:
                format = "tsv";
                break;
            case CSV:
                format = "csv";
                break;
            case TEXT:
                format = "txt";
                break;
            case JSON:
                format = "json";
                break;
            case OBJECTS:
                format = "objects";
                break;
            case TABLE:
                format = "table";
                break;
            case ROWS:
                format = "rows";
                break;
            default:
                format = "default";
        }
        return format;
    }

    private String getQueryXML(String name, PathQuery query) {
        String modelName = query.getModel().getName();
        return PathQueryBinding.marshal(query, (name != null ? name : ""), modelName,
                PathQuery.USERPROFILE_VERSION);
    }
}
