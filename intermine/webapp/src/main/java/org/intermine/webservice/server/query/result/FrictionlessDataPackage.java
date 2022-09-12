package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathLengthComparator;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.exceptions.ServiceException;

/**
 * Exports Frictionless Data Package if the parameter exportDataPackage
 * is true. Summarises details about columns, data sources, primary keys.
 *
 * @author Nikhil Vats
 */
public class FrictionlessDataPackage
{
    protected LinkedHashMap<String, Object> dataPackageAttributes
            = new LinkedHashMap<String, Object>();

    protected static final String DATAPACKAGE_FILENAME = "datapackage.json";
    /**
     *
     * @param pq pathquery to export data package for
     * @param request the servlet request
     * @param executor the path query executor
     * @param format format of the results file
     */
    protected void exportDataPackage(PathQuery pq, HttpServletRequest request,
        PathQueryExecutor executor, String format) {

        String clsName; // the name of root class
        try {
            clsName = pq.getRootClass();
        } catch (PathException e1) {
            // Check this
            throw new ServiceException(e1);
        }

        // array of objects (column details)
        ArrayList<Map<String, Object>> fields = new ArrayList<>();

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
                String friendlyPath = WebUtil.formatPathDescription(v,
                        pq, InterMineContext.getWebConfig());

                // make the column details object
                columnDetails.put("name", p.getLastElement());
                columnDetails.put("type", type);
                columnDetails.put("class path", friendlyPath);
                columnDetails.put("class ontology link",
                        p.getLastClassDescriptor().getOntologyTerm());
                columnDetails.put("attribute ontology link",
                        ((AttributeDescriptor) p.getEndFieldDescriptor()).getOntologyTerm());

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

        // get web service url for this query
        String xml = getQueryXML(null, pq);
        String serviceFormat;
        if (request.getParameter("serviceFormat") != null) {
            serviceFormat = request.getParameter("serviceFormat");
        } else {
            serviceFormat = "tab";
        }
        String link = new QueryResultLinkGenerator().getLink(
                new URLGenerator(request).getPermanentBaseURL(), xml,
                serviceFormat);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        String mineName = InterMineContext.getWebProperties().getProperty("webapp.path");
        String mineVersion = InterMineContext.getWebProperties().getProperty(
                "project.releaseVersion");
        // make the resource object of resources array
        LinkedHashMap<String, Object> resource = new LinkedHashMap<String, Object>();
        resource.put("profile", "tabular-data-resource");
        resource.put("name", mineName + "-query-data-resource");
        resource.put("path", link);
        resource.put("format", format);
        resource.put("timestamp", timestamp);
        resource.put("schema", schema);

        // the resources array always contains only 1 resource in our case
        ArrayList<Object> resources = new ArrayList<Object>();
        resources.add(resource);

        ArrayList<Object> dataSources = new ArrayList<Object>();

        // finally, prepare the data package object to be exported
        dataPackageAttributes.put("profile", "tabular-data-package");
        dataPackageAttributes.put("name", mineName + "@v" + mineVersion);
        dataPackageAttributes.put("resources", resources);

        try {
            PathQuery cloneQuery = pq.clone();
            PathQuery newPq = processQuery(cloneQuery);
            List<String> originalViews = pq.getView();
            List<Path> viewPaths = new ArrayList<Path>();
            for (String v : originalViews) {
                try {
                    viewPaths.add(pq.makePath(v));
                } catch (PathException e) {
                    throw new RuntimeException("Problem making path " + v, e);
                }
            }

            String namePath = viewPaths.get(0).getStartClassDescriptor().getUnqualifiedName()
                    + ".dataSets.dataSource.name";

            if (viewPaths.get(0).getStartClassDescriptor().getAllSuperclassNames().contains(
                    "org.intermine.model.bio.BioEntity")) {
                List<ResultsRow> nameList = (List) executor.summariseQuery(newPq, namePath, true);

                PathQuery dataSourceQuery = new PathQuery(Model.getInstanceByName("genomic"));
                dataSourceQuery.addViews("DataSource.name", "DataSource.url");

                String constraintLogic = "";
                List<String> simpleList = new ArrayList<>();
                String name;
                for (int i = 0; i < nameList.size(); i++) {
                    name = nameList.get(i).get(0).toString();
                    dataSourceQuery.addConstraint(Constraints.eq("DataSource.name", name),
                            String.valueOf(Character.toChars(i + 65)));
                    simpleList.add(name);
                    if (i < nameList.size() - 1) {
                        constraintLogic += String.valueOf(Character.toChars(i + 65)) + " OR ";
                    } else {
                        constraintLogic += String.valueOf(Character.toChars(i + 65));
                    }
                }
                dataSourceQuery.setConstraintLogic(constraintLogic);

                ExportResultsIterator results;
                try {
                    results = executor.execute(dataSourceQuery);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException(e);
                }

                while (results.hasNext()) {
                    List<ResultElement> item = results.next();
                    String sourceName = item.get(0).getField().toString();
                    //in case of data source names duplication (Panther, panther) we want
                    //only to add that ones returned by summariseQuery
                    if (simpleList.contains(sourceName)) {
                        LinkedHashMap<String, String> tempDataSource =
                                new LinkedHashMap<String, String>();
                        tempDataSource.put("title", sourceName);
                        tempDataSource.put("url", item.get(1).getField() != null
                                ? item.get(1).getField().toString() : "null");
                        dataSources.add(tempDataSource);
                    }
                }

                dataPackageAttributes.put("sources", dataSources);
            }
        } catch (ObjectStoreException e) {
            throw new ServiceException(e);
        }
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
        Map<String, List<FieldDescriptor>> classKeys
                = ClassKeyHelper.readKeys(pq.getModel(), props);
        List<String> keys = ClassKeyHelper.getKeyFieldNames(classKeys, clsName);
        return keys;
    }

    private String getQueryXML(String name, PathQuery query) {
        String modelName = query.getModel().getName();
        return PathQueryBinding.marshal(query, (name != null ? name : ""), modelName,
                PathQuery.USERPROFILE_VERSION);
    }

    /**
     * Transform a query from a standard one into one that conforms to the requirements
     * of JSON objects.
     * @param beforeChanges The query to transform.
     * @return A transformed query.
     */
    public static PathQuery processQuery(PathQuery beforeChanges) {
        PathQuery afterChanges = beforeChanges.clone();
        afterChanges.clearView();
        afterChanges.clearOrderBy();
        List<String> newViews = getAlteredViews(beforeChanges);
        afterChanges.addOrderBy(new OrderElement(newViews.get(0), OrderDirection.ASC));
        afterChanges.addViews(newViews);

        return afterChanges;
    }

    /**
     * Get the views for the transformed query.
     * @param pq The original query.
     * @return Its new views.
     */
    public static List<String> getAlteredViews(PathQuery pq) {
        List<String> originalViews = pq.getView();
        List<Path> viewPaths = new ArrayList<Path>();
        for (String v : originalViews) {
            try {
                viewPaths.add(pq.makePath(v));
            } catch (PathException e) {
                throw new RuntimeException("Problem making path " + v, e);
            }
        }
        Collections.sort(viewPaths, PathLengthComparator.getInstance());
        List<String> newViews = new ArrayList<String>();

        String namePath = viewPaths.get(0).getStartClassDescriptor().getUnqualifiedName()
                + ".dataSets.dataSource.name";
        String urlPath = viewPaths.get(0).getStartClassDescriptor().getUnqualifiedName()
                + ".dataSets.dataSource.url";

        newViews.add(0, namePath);
        newViews.add(1, urlPath);
        return newViews;
    }
}
