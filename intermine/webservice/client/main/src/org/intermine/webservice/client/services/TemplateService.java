package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;
import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.core.Request;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.results.Page;
import org.intermine.webservice.client.results.RowResultSet;
import org.intermine.webservice.client.template.TemplateParameter;
import org.intermine.webservice.client.util.HttpConnection;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The TemplateService represents the connection to the resource that
 * returns results of templates and number of results.
 *
 * A template is a predefined query, with a set number of configurable parameters, similar to a
 * search form. Only a subset of their actual constraints are editable, although at least one will
 * be. For example you might have a template that finds Alcohol-Dehydrogenase genes in a specific
 * organism - although this would require a couple of constraints, only the one that
 * specifies the organism need be visible to the end user.
 *
 * From the user's perspective, templates can offer two advantages:
 * <ul>
 *   <li>They can be simpler to run, as only the parts of the query relevant to the particular
 *       search need to be specified (for example you never need to set the output columns)</li>
 *   <li>They provide simple access to a saved query from anywhere the webservice is available -
 *       so while queries can be saved as XML on your own machine, as a template they can be
 *       run from anywhere</li>
 * </ul>
 *
 * There are two ways to use templates - either you can fetch a template object from the
 * service, and use that to build the request, or you can build it by referencing the parameters
 * and output. The former method is preferable as it will catch errors caused by changes to the
 * template structure earlier on, and allow you to introspect the template.
 *
 * Using a Template object:
 * <pre>
 * PrintStream out = System.out;
 *
 * ServiceFactory serviceFactory = new ServiceFactory(serviceRootUrl);
 * TemplateService templateService = serviceFactory.getTemplateService();
 *
 * // Refer to the template by its name (displayed in the browser's address bar)
 * String templateName = "ChromLocation_GeneTranscriptExon";
 * TemplateQuery template = templateService.getTemplate(templateName);
 *
 * // You only need to specify the values of the constraints you wish to alter:
 * template.replaceConstraint(template.getConstraintForCode("B"),
 * Constraints.eq("Chromosome.primaryIdentifier", "2L"));
 *
 * Iterator<List<Object>> resultSet = templateService.getRowListIterator(template, new Page(0, 10));
 *
 * out.println(StringUtils.join(template.getView(), "\t"));
 * while (resultSet.hasNext()) {
 *     out.println(StringUtils.join(resultSet.next(), "\t"));
 * }
 * </pre>
 *
 * When using the other method, it is assumed that if you are familiar with the template's
 * parameters and output. Using the name and parameter method saves a call to the service to
 * retrieve the template in the first place:
 *
 * Using template name and parameters:
 * <pre>
 * PrintStream out = System.out;
 *
 * ServiceFactory serviceFactory = new ServiceFactory(serviceRootUrl);
 * TemplateService templateService = serviceFactory.getTemplateService();
 *
 * // Refer to the template by its name (displayed in the browser's address bar)
 * String templateName = "ChromLocation_GeneTranscriptExon";
 *
 * // Specify the values for this particular request
 * List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();
 * parameters.add(new TemplateParameter("Chromosome.organism.name", "eq", "*melanogaster"));
 * parameters.add(new TemplateParameter("Chromosome.primaryIdentifier", "eq", "2L"));
 * parameters.add(new TemplateParameter("Chromosome.locatedFeatures.start", "ge", "1"));
 * parameters.add(new TemplateParameter("Chromosome.locatedFeatures.end", "lt", "10000"));
 *
 * Iterator<List<Object>> resultSet = templateService.getRowListIterator(templateName, parameters,
 *                                       new Page(0, 10));
 *
 * while (resultSet.hasNext()) {
 *     out.println(StringUtils.join(resultSet.next(), "\t"));
 * }
 * </pre>
 *
 * @author Alexis Kalderimis
 * @author Jakub Kulaviak
 **/
public class TemplateService extends AbstractQueryService<TemplateQuery>
{

    private static final String SERVICE_RELATIVE_URL = "template/results";
    private static final String AVAILABLE_TEMPLATES = "/templates";

    /**
     * Use {@link ServiceFactory} instead of constructor for creating this service.
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public TemplateService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    /**
     * A subclass of RequestImpl relevant to template queries.
     *
     * @author Alex Kalderimis
     * @author Jakub Kulaviak
     */
    protected static class TemplateRequest extends RequestImpl
    {
        /**
         * Constructor.
         *
         * @param type GET or POST
         * @param serviceUrl the URL of the service, not including parameters
         * @param contentType a ContentType object
         */
        public TemplateRequest(RequestType type, String serviceUrl, ContentType contentType) {
            super(type, serviceUrl, contentType);
        }

        /**
         * Sets the name of the template to be used.
         *
         * @param xml template name
         */
        public void setName(String xml) {
            setParameter("name", xml);
        }

        /**
         * Set some template parameters.
         *
         * @param parameters a List of TemplateParameter objects
         */
        public void setTemplateParameters(List<TemplateParameter> parameters) {
            for (int i = 0; i < parameters.size(); i++) {
                TemplateParameter par = parameters.get(i);
                int index = i + 1;
                addParameter("constraint" + index, par.getPathId());
                addParameter("op" + index, par.getOperation());
                String valueIndex = "value" + index;
                if (par.isMultiValue()) {
                    for (String value : par.getValues()) {
                        addParameter(valueIndex, value);
                    }
                } else {
                    addParameter(valueIndex, par.getValue());
                }
                if (par.getExtraValue() != null) {
                    addParameter("extra" + index, par.getExtraValue());
                }
                if (par.getCode() != null) {
                    addParameter("code" + index, par.getCode());
                }
            }
        }
    }

    /**
     * Get the names of the templates to which the current user has access.
     * @return The names of the available templates.
     */
    public Set<String> getTemplateNames() {
        return getTemplates().keySet();
    }

    /**
     * Get the templates to which the current user has access.
     * @return The templates the user can access.
     */
    public Map<String, TemplateQuery> getTemplates() {
     // Have to do this or the model won't be available at the parsing stage...

        getFactory().getModel();
        Request request = new RequestImpl(
                RequestType.GET,
                getRootUrl() + AVAILABLE_TEMPLATES,
                ContentType.TEXT_XML);
        HttpConnection connection = executeRequest(request);
        Map<String, TemplateQuery> res;
        try {
            res = TemplateQueryBinding.unmarshalTemplates(
                new InputStreamReader(connection.getResponseBodyAsStream()),
                TemplateQuery.USERPROFILE_VERSION);
        } finally {
            connection.close();
        }
        return res;
    }

    /**
     * Get the template with the given name, if accessible.
     *
     * Returns null if the template does not exist or the current user cannot
     * access it.
     * @param name The name of the template to return.
     * @return The template with the given name.
     */
    public TemplateQuery getTemplate(String name) {
        return getTemplates().get(name);
    }

    /**
     * Get all the templates with constraints which constrain paths of a certain type.
     *
     * If a template has a constraint on "Employee.name", then that template will be included
     * in the set of results for when type is "Employee", as well as when type is
     * "Manager".
     *
     * @param type The type of object to enquire about.
     * @return The set of suitable templates.
     */
    public Set<TemplateQuery> getTemplatesForType(String type) {
        if (type == null) {
            throw new NullPointerException("'type' is null in getTemplatesForType");
        }
        Model m = getFactory().getModel();
        Map<String, TemplateQuery> templates = getTemplates();
        Set<TemplateQuery> res = new HashSet<TemplateQuery>();
        for (TemplateQuery tq: templates.values()) {
            for (PathConstraint c: tq.getConstraints().keySet()) {
                Path p;
                try {
                    p = tq.makePath(c.getPath());
                } catch (PathException e) {
                    throw new ServiceException(e);
                }
                ClassDescriptor cd = p.getLastClassDescriptor();
                if (type.equals(cd.getUnqualifiedName())
                        || m.getClassDescriptorByName(type)
                            .getAllSuperDescriptors().contains(cd)) {
                    res.add(tq);
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Returns the number of results the specified template will return.
     *
     * @param templateName the name of the template to run
     * @param parameters the values for the templates editable constraints
     * @return number of results of specified query.
     */
    public int getCount(String templateName, List<TemplateParameter> parameters) {
        TemplateRequest request =
            new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_COUNT);

        request.setName(templateName);
        request.setTemplateParameters(parameters);
        return getIntResponse(request);
    }

    @Override
    public int getCount(TemplateQuery template) {
        List<TemplateParameter> parameters = getParametersFor(template);
        return getCount(template.getName(), parameters);
    }

    private List<TemplateParameter> getParametersFor(TemplateQuery template) {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        for (PathConstraint pc: template.getEditableConstraints()) {
            if (template.getSwitchOffAbility(pc) != SwitchOffAbility.OFF) {
                TemplateParameter tp;
                String path = pc.getPath();
                String op = pc.getOp().toString();
                String code = template.getConstraints().get(pc);
                if (PathConstraint.getValues(pc) != null) {
                    tp = new TemplateParameter(path, op, PathConstraint.getValues(pc), code);
                } else {
                    tp = new TemplateParameter(path, op, PathConstraint.getValue(pc), PathConstraint.getExtraValue(pc), code);
                }
                params.add(tp);
            }
        }
        return params;
    }

    /**
     * Returns all the results for the given template template with the given parameters.
     * If you expect a lot of results we would recommend you use getAllResultIterator() method.
     *
     * @param templateName template name
     * @param parameters parameters of template
     *
     * @return results
     */
    public List<List<String>> getAllResults(String templateName,
            List<TemplateParameter> parameters) {
        return getResults(templateName, parameters, Page.DEFAULT);
    }

    /**
     * Returns a set of the results for the given template template with the given parameters,
     * defined by the index of the first result you want back, and the maximum page size.
     * If you expect a lot of results we would recommend you use getResultIterator() method.
     *
     * @param templateName template name
     * @param parameters parameters of template
     * @param page The subsection of the result set to retrieve.
     *
     * @return A result set starting at the given index and no larger than the maximum page size.
     */
    public List<List<String>> getResults(String templateName, List<TemplateParameter> parameters,
            Page page) {
        TemplateRequest request =
                new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_XML);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        request.setPage(page);
        return getResponseTable(request).getData();
    }

    @Override
    public List<List<String>> getResults(TemplateQuery template, Page page) {
        List<TemplateParameter> parameters = getParametersFor(template);
        return getResults(template.getName(), parameters, page);
    }

    /**
     * Returns all the results for the given template template with the given parameters,
     * as JSON objects (see @link {http://www.intermine.org/wiki/JSONObjectFormat}).
     *
     * @param templateName template name
     * @param parameters parameters of template
     *
     * @return results
     *
     * @throws JSONException if the server returns content that cannot be parsed as JSON
     */
    public List<JSONObject> getAllJSONResults(String templateName,
            List<TemplateParameter> parameters) throws JSONException {
        return getJSONResults(templateName, parameters, Page.DEFAULT);
    }

    /**
     * Returns a subset of the results for the given template template with the given parameters,
     * as JSON objects (see @link {http://www.intermine.org/wiki/JSONObjectFormat}).
     *
     * @param templateName template name
     * @param parameters parameters of template
     * @param page The subsection of the result set to retrieve.
     *
     * @return a list of JSON objects
     *
     * @throws JSONException if the server returns content that cannot be parsed as JSON
     */
    public List<JSONObject> getJSONResults(String templateName,
            List<TemplateParameter> parameters, Page page) throws JSONException {
        TemplateRequest request =
                new TemplateRequest(RequestType.POST, getUrl(), ContentType.APPLICATION_JSON_OBJ);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        request.setPage(page);
        return getJSONResponse(request).getObjects();
    }

    @Override
    public List<JSONObject> getJSONResults(TemplateQuery template, Page page)
        throws JSONException {
        List<TemplateParameter> parameters = getParametersFor(template);
        return getJSONResults(template.getName(), parameters, page);
    }

    /**
     * Returns all the rows for the template when run with the given parameters.
     *
     * Use this method if you expect a lot of results and you would otherwise run out of memory.
     *
     * @param templateName template name
     * @param parameters parameters of template
     *
     * @return results as an iterator over lists of strings
     */
    public Iterator<List<String>> getAllRowsIterator(String templateName,
            List<TemplateParameter> parameters) {
        return getRowIterator(templateName, parameters, Page.DEFAULT);
    }

    /**
     * Returns an iterator over a subset of rows for the template
     * when run with the given parameters.
     *
     * Use this method if you expect a lot of results and you would otherwise
     * run out of memory.
     *
     * @param templateName template name
     * @param parameters parameters of template
     * @param page The subsection of the result set to retrieve.
     *
     * @return results as an iterator over lists of strings
     */
    public Iterator<List<String>> getRowIterator(String templateName,
            List<TemplateParameter> parameters, Page page) {
        TemplateRequest request =
                new TemplateRequest(RequestType.POST, getUrl(), ContentType.TEXT_XML);
        request.setName(templateName);
        request.setTemplateParameters(parameters);
        request.setPage(page);
        return getResponseTable(request).getIterator();
    }

    @Override
    public Iterator<List<String>> getRowIterator(TemplateQuery template, Page page) {
        List<TemplateParameter> parameters = getParametersFor(template);
        return getRowIterator(template.getName(), parameters, page);
    }

    /**
     * Get results for a query as rows of objects.
     *
     * @param name The name of the template to run.
     * @param params The settings for the various template constraints.
     * @param page The subsection of the result set to retrieve.
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(String name,
            List<TemplateParameter> params, Page page) {
        return getRows(name, params, page).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects. Retrieve up to
     * 10,000,000 rows from the beginning.
     *
     * @param name The name of the template to run.
     * @param params The settings for the various template constraints.
     *
     * @return a list of rows, which are each a list of cells.
     */
    public List<List<Object>> getRowsAsLists(String name, List<TemplateParameter> params) {
        return getRows(name, params, Page.DEFAULT).getRowsAsLists();
    }

    /**
     * Get results for a query as rows of objects.
     *
     * @param name The name of the template to run.
     * @param params The settings for the various template constraints.
     * @param page The subsection of the result set to retrieve.
     * @return a list of rows, which are each a map from output column
     * (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(String name, List<TemplateParameter> params,
            Page page) {
        return getRows(name, params, page).getRowsAsMaps();
    }

    /**
     * Get results for a query as rows of objects.
     * Get up to the maximum result size of 10,000,000 rows from the beginning.
     *
     * @param name The name of the template to run.
     * @param params The settings for the various template constraints.
     *
     * @return a list of rows, which are each a map from
     * output column (in alternate long and short form) to value.
     */
    public List<Map<String, Object>> getRowsAsMaps(String name, List<TemplateParameter> params) {
        return getRows(name, params, Page.DEFAULT).getRowsAsMaps();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection.
     *
     *
     * @param name The name of the template to run.
     * @param params The settings for the various template constraints.
     * @param page The subsection of the result set to retrieve.
     * @return an iterator over the rows, where each row is a list of objects.
     */
    public Iterator<List<Object>> getRowListIterator(String name, List<TemplateParameter> params,
            Page page) {
        return getRows(name, params, page).getListIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection. Retrieves up to the maximum
     * result size of 10,000,000 rows from the beginning.
     *
     *
     * @param name The name of the template to run.
     * @param params The settings for the various template constraints.
     *
     * @return an iterator over the rows, where each row is a list of objects.
     */
    public Iterator<List<Object>> getRowListIterator(String name, List<TemplateParameter> params) {
        return getRows(name, params, Page.DEFAULT).getListIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection.
     *
     * @param name The name of the template to run.
     * @param params The settings for the various template constraints.
     * @param page The subsection of the result set to retrieve.
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(String name,
            List<TemplateParameter> params, Page page) {
        return getRows(name, params, page).getMapIterator();
    }

    /**
     * Get an iterator over the results of a query. The iterator returns a representation
     * of one row at a time, in the order received over the connection,
     * up to the maximum result size of 10,000,000 rows from the beginning.
     *
     * @param name The name of the template to run.
     * @param params The settings for the various template constraints.
     *
     * @return an iterator over the rows, where each row is a mapping from output column to value.
     */
    public Iterator<Map<String, Object>> getRowMapIterator(String name,
            List<TemplateParameter> params) {
        return getRows(name, params, Page.DEFAULT).getMapIterator();
    }

    private RowResultSet getRows(String name, List<TemplateParameter> params, Page page) {
        TemplateQuery tq = getTemplate(name);
        if (tq == null) {
            throw new ServiceException("There is no template named " + name);
        }
        return getRows(name, params, tq.getView(), page);
    }

    @Override
    protected RowResultSet getRows(TemplateQuery query, Page page) {
        List<TemplateParameter> parameters = getParametersFor(query);
        return getRows(query.getName(), parameters, query.getView(), page);
    }

    private RowResultSet getRows(String name, List<TemplateParameter> params,
            List<String> views, Page page) {
        ContentType ct = (getAPIVersion() < 8) ? ContentType.APPLICATION_JSON_ROW : ContentType.APPLICATION_JSON;
        TemplateRequest request = new TemplateRequest(RequestType.POST, getUrl(), ct);

        request.setName(name);
        request.setTemplateParameters(params);
        return getRows(request, views);
    }

}
