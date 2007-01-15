package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionErrors;
import org.intermine.cache.InterMineCache;
import org.intermine.cache.ObjectCreator;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.util.TypeUtil;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.results.InlineTemplateTable;
import org.intermine.web.results.PagedResults;
import org.intermine.web.results.WebResults;

/**
 * Static helper routines related to templates.
 *
 * @author  Thomas Riley
 */
public class TemplateHelper
{
    private static final Logger LOG = Logger.getLogger(TemplateHelper.class);

    /** Type parameter indicating globally shared template. */
    public static final String GLOBAL_TEMPLATE = "global";
    /** Type parameter indicating group shared template. */
    public static final String SHARED_TEMPLATE = "shared";
    /** Type parameter indicating private user template. */
    public static final String USER_TEMPLATE = "user";
    /** Type parameter indicating ALL templates */
    public static final String ALL_TEMPLATE = "all";
    /** Type parameter indicating temporary templates **/
    public static final String TEMP_TEMPLATE = "temp";

    /**
     * Locate TemplateQuery by identifier. The type parameter
     * @param servletContext the ServletContext
     * @param session the HttpSession
     * @param userName the user name (for finding user templates)
     * @param templateName  template query identifier/name
     * @param type        type of tempate, either GLOBAL_TEMPLATE, SHARED_TEMPLATE or USER_TEMPLATE,
     *                    ALL_TEMPLATE
     * @return            the located template query with matching identifier
     */
    public static TemplateQuery findTemplate(ServletContext servletContext,
                                             HttpSession session,
                                             String userName,
                                             String templateName,
                                             String type) {

        ProfileManager pm =
            (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        Profile profile = null;
        if (userName != null) {
            profile = pm.getProfile(userName);
        }
        if (profile == null && session != null) {
            profile = (Profile) session.getAttribute(Constants.PROFILE);
        }
        if (profile == null || GLOBAL_TEMPLATE.equals(type)) {
            Map templates =
                SessionMethods.getSuperUserProfile(servletContext).getSavedTemplates();
            return (TemplateQuery) templates.get(templateName);
        } else if (USER_TEMPLATE.equals(type)) {
            return (TemplateQuery) profile.getSavedTemplates().get(templateName);
        } else if (SHARED_TEMPLATE.equals(type)) {
            // TODO implement shared templates
            return null;
        } else if (ALL_TEMPLATE.equals(type)) {
            TemplateQuery tq = findTemplate(servletContext, session, userName,
                                            templateName, GLOBAL_TEMPLATE);
            if (tq == null) {
                return findTemplate(servletContext, session, userName, templateName, USER_TEMPLATE);
            } else {
                return tq;
            }
        } else if (TEMP_TEMPLATE.equals(type)) {
           SavedQuery savedQuery = (SavedQuery) profile.getHistory().get(templateName);
           return (TemplateQuery) savedQuery.getPathQuery();
        } else {
            throw new IllegalArgumentException("type: " + type);
        }
    }

    /**
     * Create a new TemplateQuery with input submitted by user contained within
     * a TemplateForm bean.
     *
     * @param tf        the template form bean
     * @param template  the template query involved
     * @param savedBags the saved bags
     * @return          a new TemplateQuery matching template with user supplied constraints
     */
    public static TemplateQuery templateFormToTemplateQuery(TemplateForm tf,
                                                            TemplateQuery template,
                                                            Map savedBags) {
        TemplateQuery queryCopy = (TemplateQuery) template.clone();

        // if this query comes from current query it may have been altered
        // to use a bag constraint

        // Step over nodes and their constraints in order, ammending our
        // PathQuery copy as we go
        int j = 0;
        for (Iterator i = template.getEditableNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            for (Iterator ci = template.getEditableConstraints(node).iterator(); ci.hasNext();) {
                Constraint c = (Constraint) ci.next();
                String key = "" + (j + 1);
                PathNode nodeCopy = (PathNode) queryCopy.getNodes().get(node.getPath());

                if (tf.getUseBagConstraint(key)) {
                    // Replace constraint with bag constraint
                    ConstraintOp constraintOp = ConstraintOp.
                        getOpForIndex(Integer.valueOf(tf.getBagOp(key)));
                    Object constraintValue = tf.getBag(key);
                    // if using an id bag need to swap for a constraint on id
                    InterMineBag bag;
                    if (constraintValue instanceof InterMineBag) {
                        bag = (InterMineBag) constraintValue;
                    } else {
                        bag = (InterMineBag) savedBags.get(constraintValue);
                    }
                    if (bag != null) {
                        // constrain parent object of this node to be in bag
                        PathNode parent = (PathNode) queryCopy.getNodes()
                            .get(nodeCopy.getParent().getPath());
                        Constraint bagConstraint = new Constraint(constraintOp, constraintValue,
                                true, c.getDescription(), c.getCode(), c.getIdentifier());
                        parent.getConstraints().add(bagConstraint);

                        // remove the constraint on this node, possibly remove node
                        //nodeCopy.getConstraints().remove(node.getConstraints().indexOf(c));
                        if (nodeCopy.getConstraints().size() == 1) {
                            queryCopy.getNodes().remove(nodeCopy.getPath());
                        }
                    } else {
                        nodeCopy.getConstraints().set(
                                node.getConstraints().indexOf(c),
                                new Constraint(constraintOp, constraintValue,
                                        true, c.getDescription(), c.getCode(),
                                        c.getIdentifier()));
                    }

                } else {
                    // Parse user input
                    String op = (String) tf.getAttributeOps(key);
                    ConstraintOp constraintOp = ConstraintOp.getOpForIndex(Integer.valueOf(op));
                    Object constraintValue = tf.getParsedAttributeValues(key);

                    // In query copy, replace old constraint with new one
                    nodeCopy.getConstraints().set(node.getConstraints().indexOf(c),
                            new Constraint(constraintOp, constraintValue, true,
                                    c.getDescription(), c.getCode(), c.getIdentifier()));
                }
                j++;
            }
        }

        // Set the desired view list
        if (!StringUtils.isEmpty(tf.getView())) {
            queryCopy.setView(template.getAlternativeView(tf.getView()));
        }

        queryCopy.setEdited(true);
        return queryCopy;
    }

    /**
     * Given a Map of TemplateQuerys (mapping from template name to TemplateQuery)
     * return a string containing each template seriaised as XML. The root element
     * will be a <code>template-list</code> element.
     *
     * @param templates  map from template name to TemplateQuery
     * @return  all template queries serialised as XML
     * @see  TemplateQuery
     */
    public static String templateMapToXml(Map templates) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        Iterator iter = templates.values().iterator();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("template-list");
            while (iter.hasNext()) {
                TemplateQueryBinding.marshal((TemplateQuery) iter.next(), writer);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }

    /**
     * Parse templates in XML format and return a map from template name to
     * TemplateQuery.
     *
     * @param xml  the template queries in xml format
     * @return     Map from template name to TemplateQuery
     * @throws Exception  when a parse exception occurs (wrapped in a RuntimeException)
     */
    public static Map xmlToTemplateMap(String xml) throws Exception {
        Reader templateQueriesReader = new StringReader(xml);
        return new TemplateQueryBinding().unmarshal(templateQueriesReader);
    }

    /**
     * Build a template query given a TemplateBuildState and a PathQuery
     *
     * @param tbs the template build state
     * @param query the path query
     * @return a template query
     */
    public static TemplateQuery buildTemplateQuery(TemplateBuildState tbs, PathQuery query) {
        TemplateQuery template = new TemplateQuery(tbs.getName(),
                                                   tbs.getTitle(),
                                                   tbs.getDescription(),
                                                   tbs.getComment(),
                                                   (PathQuery) query.clone(), tbs.isImportant(),
                                                   tbs.getKeywords());
        return template;
    }


    /**
     * Try to fill the TemplateForm argument using the attribute values in the InterMineObject
     * arg and return the number of form fields that aren't set afterwards.
     */
    private static int fillTemplateForm(TemplateQuery template, //String viewName,
                                        InterMineObject object,
                                        InterMineBag bag,
                                        TemplateForm templateForm, Model model) {
        List constraints = template.getAllConstraints();
        int unmatchedConstraintCount = constraints.size();
        String equalsString = ConstraintOp.EQUALS.getIndex().toString();
        String inString = ConstraintOp.IN.getIndex().toString();

        //templateForm.setView(viewName);

        Iterator constraintIter = constraints.iterator();
        while (constraintIter.hasNext()) {
            Constraint c = (Constraint) constraintIter.next();

            if (!c.isEditable()) {
                // this constraint doesn't need to be filled in
                unmatchedConstraintCount--;
                continue;
            }
            
            String constraintIdentifier = c.getIdentifier();
            String[] bits = constraintIdentifier.split("\\.");

            int formIndex = 1;
            
            if (bits.length == 2) {
                String className = model.getPackageName() + "." + bits[0];
                String fieldName = bits[1];

                try {
                    Class testClass = Class.forName(className);

                    if (object != null && testClass.isInstance(object)) {
                        ClassDescriptor cd = model.getClassDescriptorByName(className);
                        if (cd.getFieldDescriptorByName(fieldName) != null) {
                            Object fieldValue = TypeUtil.getFieldValue(object, fieldName);

                            if (fieldValue == null) {
                                // this field is not a good constraint value
                                continue;
                            }

                            unmatchedConstraintCount--;

                            templateForm.setAttributeOps("" + formIndex, equalsString);
                            templateForm.setAttributeValues("" + formIndex, fieldValue);

                            formIndex++;                            
                        }
                    }
                    if (bag != null && (TypeUtil.unqualifiedName(testClass.toString()))
                                    .equals(bag.getType())) {
                        unmatchedConstraintCount--;
                        templateForm.setBagOp("" + formIndex, inString);
                        templateForm.setBag("" + formIndex, bag);
                        templateForm.setUseBagConstraint("" + formIndex, true);
                    }
                } catch (ClassNotFoundException e) {
                    LOG.error(e);
                } catch (IllegalAccessException e) {
                    LOG.error(e);
                }
            }
        }

        return unmatchedConstraintCount;
    }


    /**
     * Make and return an InlineTemplateTable for the given template and interMineObjectId or
     * InterMineIdBag
     */
    private static InlineTemplateTable makeInlineTemplateTable(ServletContext servletContext,
                                                               TemplateQuery template,
                                                               InterMineObject object,
                                                               InterMineBag bag) {
        TemplateForm templateForm = new TemplateForm();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Map webProperties = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);

        /*if (template.getQuery().getAlternativeView(viewName) == null) {
            // ignore templates that don't have an attributes only view
            return null;
        }*/

        int unconstrainedCount =
            fillTemplateForm(template, /*viewName,*/ object, bag, templateForm, os.getModel());
        if (unconstrainedCount > 0) {
            return null;
        }

        templateForm.parseAttributeValues(template, null, new ActionErrors(), false);

         PathQuery pathQuery = TemplateHelper.templateFormToTemplateQuery(templateForm, template,
                                                                          new HashMap());
        try {
            Map pathToQueryNode = new HashMap();
            Query query = MainHelper.makeQuery(pathQuery, Collections.EMPTY_MAP, pathToQueryNode);
            Results results = os.execute(query);
            Model model = os.getModel();
            WebResults webResults =
                new WebResults(pathQuery.getViewAsPaths(), results, model, pathToQueryNode,
                               (Map) servletContext.getAttribute(Constants.CLASS_KEYS));
            PagedResults pagedResults = new PagedResults(webResults);

            InlineTemplateTable itt =
                new InlineTemplateTable(pagedResults, webProperties);

            /*Iterator viewIter = viewNodes.iterator();
            while (viewIter.hasNext()) {
                String path = (String) viewIter.next();
                String className = MainHelper.getTypeForPath(path, pathQuery);
                if (className.indexOf(".") == -1) {
                    // a primative like "int"
                } else {
                    Class nodeClass = Class.forName(className);

                    if (InterMineObject.class.isAssignableFrom(nodeClass)) {
                        // can't display objects inline yet
                        //return null;
                    }
                }
            }*/
            return itt;

        } catch (IllegalArgumentException e) {
            // probably a template is out of date
            LOG.error("error while getting inline template information", e);
        //} catch (ClassNotFoundException e) {
        //    // probably a template is out of date
        //    LOG.error("error while getting inline template information", e);
        } catch (ObjectStoreException e) {
            LOG.error("error while getting inline template information", e);
            throw new RuntimeException("error while getting inline template information", e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ObjectStoreException) {
                // special case: if there is an object store problem it's probably an
                // ObjectStoreQueryDurationException - returning null will cause the template to
                // be run again later when, hopefully, the genetic query optimiser will choose a
                // better plan
                return null;
            }
        }

        return null;
    }


    /**
     * The cache tag to use when looking for template tables in the cache.
     */
    public static final String TEMPLATE_TABLE_CACHE_TAG = "template_table_tag";

    private static final String NO_USERNAME_STRING = "__NO_USER_NAME__";

    /**
     * Register an ObjectCreator for creating inline template tables.
     * @param cache the InterMineCache
     * @param servletContext the ServletContext
     */
    public static void registerTemplateTableCreator(InterMineCache cache,
                                                    final ServletContext servletContext) {
        ObjectCreator templateTableCreator = new ObjectCreator() {
            final ObjectStore os =
                (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

            public Serializable create(String templateName, /*String viewName,*/
                                       InterMineBag interMineIdBag, String userName) {
                if (userName.equals(NO_USERNAME_STRING)) {
                    // the create method can't have a null argument, but null is the signal for
                    // findTemplate() that there is no current user
                    userName = null;
                }
                TemplateQuery template =
                    TemplateHelper.findTemplate(servletContext, null, userName,
                                                templateName, TemplateHelper.ALL_TEMPLATE);

                    if (template == null) {
                        throw new IllegalStateException("Could not find template \""
                                                        + templateName + "\"");
                    }

                InterMineObject object;
//                try {
//                    object = os.getObjectById(id);
//                } catch (ObjectStoreException e) {
//                    throw new RuntimeException("cannot find object for ID: " + id);
//                }
                return makeInlineTemplateTable(servletContext, template, /*viewName,*/ null,
                                               interMineIdBag);
            };
            public Serializable create(String templateName, /*String viewName,*/
                                       Integer id, String userName) {
                if (userName.equals(NO_USERNAME_STRING)) {
                    // the create method can't have a null argument, but null is the signal for
                    // findTemplate() that there is no current user
                    userName = null;
                }
                TemplateQuery template =
                    TemplateHelper.findTemplate(servletContext, null, userName,
                                                templateName, TemplateHelper.ALL_TEMPLATE);

                    if (template == null) {
                        throw new IllegalStateException("Could not find template \""
                                                        + templateName + "\"");
                    }

                InterMineObject object;
                try {
                    object = os.getObjectById(id);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("cannot find object for ID: " + id);
                }
                return makeInlineTemplateTable(servletContext, template, /*viewName,*/ object,
                                               null);
            }
        };

        cache.register(TEMPLATE_TABLE_CACHE_TAG, templateTableCreator);
    }

    /**
     * Make (or find in the global cache) and return an InlineTemplateTable for the given
     * template, InterMineIdBag and user name.
     * @param servletContext the ServletContext
     * @param templateName the template name
     * @param interMineIdBag the InterMineIdBag
     * @param userName the user name
     * @return the InlineTemplateTable
     */
    public static InlineTemplateTable getInlineTemplateTable(ServletContext servletContext,
                                                             String templateName,
                                                             InterMineBag interMineIdBag,
                                                             String userName) {
        if (userName == null) {
            // the ObjectCreator.create() method can't have a null argument, but null is the signal
            // for findTemplate() that there is no current user
            userName = NO_USERNAME_STRING;
        }

        InterMineCache cache = ServletMethods.getGlobalCache(servletContext);
        return (InlineTemplateTable) cache.get(TemplateHelper.TEMPLATE_TABLE_CACHE_TAG,
                                               templateName, interMineIdBag, userName);
    }

    /**
     * Make (or find in the global cache) and return an InlineTemplateTable for the given
     * template, interMineObjectId and user name.
     * @param servletContext the ServletContext
     * @param templateName the template name
     * @param id the object Id
     * @param userName the user name
     * @return the InlineTemplateTable
     */
    public static InlineTemplateTable getInlineTemplateTable(ServletContext servletContext,
                                                             String templateName,
                                                             Integer id,
                                                             String userName) {
        if (userName == null) {
            // the ObjectCreator.create() method can't have a null argument, but null is the signal
            // for findTemplate() that there is no current user
            userName = NO_USERNAME_STRING;
        }

        InterMineCache cache = ServletMethods.getGlobalCache(servletContext);
        return (InlineTemplateTable) cache.get(TemplateHelper.TEMPLATE_TABLE_CACHE_TAG,
                                               templateName, id, userName);
    }


    /**
     * Clone for operations that need to alter a template but not change the original,
     * for example when removing constraints for precomputing.
     * @param template the query to clone
     * @return a clone of the original template
     */
    // public static TemplateQuery cloneTemplate(TemplateQuery template) {
    // PathQuery queryClone = (PathQuery) template.clone();
    //
    // TemplateQuery clone = new TemplateQuery(template.getName(),
    // template.getDescription(),
    // queryClone, template.isImportant(),
    // template.getKeywords());
    // return clone;
    // }

    /**
     * Get an ObjectStore query to precompute this template - remove editable constraints
     * and add fields to select list if necessary.  Fill in indexes list with QueryNodes
     * to create additional indexes on (i.e. those added to select list).  Original
     * template is left unaltered.
     *
     * @param template to generate precompute query for
     * @param indexes any additional indexes to be created will be added to this list.
     * @return the query to precompute
     */
    public static Query getPrecomputeQuery(TemplateQuery template, List indexes) {
        return TemplateHelper.getPrecomputeQuery(template, indexes, null);
    }


    /**
     * Get an ObjectStore query to precompute this template - remove editable constraints
     * and add fields to select list if necessary.  Fill in indexes list with QueryNodes
     * to create additional indexes on (i.e. those added to select list).  Original
     * template is left unaltered.
     *
     * @param template to generate precompute query for
     * @param indexes any additional indexes to be created will be added to this list.
     * @param groupByNode a PathNode to group by, for summary data, or null for a precompute query
     * @return the query to precompute
     */
    public static Query getPrecomputeQuery(TemplateQuery template, List indexes,
            PathNode groupByNode) {
        // generate query with editable constraints removed
        TemplateQuery templateClone = template.cloneWithoutEditableConstraints();


        // dummy call to makeQuery() to fill in pathToQueryNode map
        List indexPaths = new ArrayList();
        // find nodes with editable constraints to index and possibly add to select list
        Iterator niter = template.getEditableNodes().iterator();
        while (niter.hasNext()) {
            PathNode node = (PathNode) niter.next();
            // look for editable constraints
            List ecs = template.getEditableConstraints(node);
            if (ecs != null && ecs.size() > 0) {
                // NOTE: at one point this exhibited a bug where aliases were repeated
                // in the generated query, seems to be fixed now though.
                String path = node.getPath();
                Set view = new HashSet(templateClone.getView());
                if (!view.contains(path)) {
                    templateClone.getView().add(path);
                }
                indexPaths.add(path);
            }
        }

        HashMap pathToQueryNode = new HashMap();
        Query query = MainHelper.makeQuery((PathQuery) templateClone, new HashMap(),
                pathToQueryNode);
        query.clearOrderBy();
        if (groupByNode != null) {
            query.clearSelect();
            QueryNode qn = (QueryNode) pathToQueryNode.get(groupByNode.getPath());
            query.addToSelect(qn);
            query.addToGroupBy(qn);
        } else {
            // Queries only select objects, need to add editable constraints to select so they can
            // be indexed in precomputed table.  Create additional indexes for fields.
            Iterator indexIter = indexPaths.iterator();
            while (indexIter.hasNext()) {
                String path = (String) indexIter.next();
                query.addToSelect((QueryNode) pathToQueryNode.get(path));
                indexes.add(pathToQueryNode.get(path));
            }
        }
        return query;
    }
}
