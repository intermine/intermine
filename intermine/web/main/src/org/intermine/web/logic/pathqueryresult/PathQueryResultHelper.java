package org.intermine.web.logic.pathqueryresult;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.CollectionUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;

/**
 * Helper for everything related to PathQueryResults
 *
 * @author Xavier Watkins
 */
public final class PathQueryResultHelper
{
    private PathQueryResultHelper() {
        //disable external instantiation
    }

    private static final Logger LOG = Logger.getLogger(PathQueryResultHelper.class);

    /**
     * Return a list of string paths that are defined as WebConfig to be shown in results.  This
     * will include only attributes of the given class and not follow references.  Optionally
     * provide a prefix to for creating a view for references/collections.
     *
     * @param type the class name to create a view for
     * @param model the model
     * @param webConfig we configuration
     * @param startingPath a path to prefix the class, can be null
     * @return the configured view paths for the class
     */
    public static List<String> getDefaultViewForClass(String type, Model model, WebConfig webConfig,
            String startingPath) {
        String prefix = startingPath;
        List<String> view = new ArrayList<String>();
        ClassDescriptor cld = model.getClassDescriptorByName(type);
        List<FieldConfig> fieldConfigs = FieldConfigHelper.getClassFieldConfigs(webConfig, cld);
        if (!StringUtils.isEmpty(prefix)) {
            try {
                // we can't add a subclass constraint, type must be same as the end of the prefix
                Path prefixPath = new Path(model, prefix);
                String prefixEndType = TypeUtil.unqualifiedName(prefixPath.getEndType().getName());
                if (!prefixEndType.equals(type)) {
                    throw new IllegalArgumentException("Mismatch between end type of prefix: "
                            + prefixEndType + " and type parameter: " + type);
                }
            } catch (PathException e) {
                LOG.error("Invalid path configured in webconfig for class: " + type);
            }
        } else {
            prefix = type;
        }

        for (FieldConfig fieldConfig : fieldConfigs) {
            String relPath = fieldConfig.getFieldExpr();
            // only add attributes, don't follow references, following references can be problematic
            // when subclasses get involved.
            if (fieldConfig.getShowInResults()) {
                try {
                    Path path = new Path(model, prefix + "." + relPath);
                    if (path.isOnlyAttribute()) {
                        view.add(path.getNoConstraintsString());
                    }
                } catch (PathException e) {
                    LOG.error("Invalid path configured in webconfig for class: " + type);
                }
            }
        }
        if (view.size() == 0) {
            for (AttributeDescriptor att : cld.getAllAttributeDescriptors()) {
                if (!"id".equals(att.getName())) {
                    view.add(prefix + "." + att.getName());
                }
            }
        }
        return view;
    }


    /**
     * Alias for getDefaultViewForClass, with the difference that it does not take a starting path.
     * @param type
     * @param model
     * @param webConfig
     * @param startingPath
     * @return A view list containing only attributes on the given class
     */
    private static List<String> getAttributeViewForClass(String type, Model model,
            WebConfig webConfig) {
        return getDefaultViewForClass(type, model, webConfig, null);
    }

    /**
     * Create a PathQuery to get the contents of an InterMineBag
     *
     * @param imBag the bag
     * @param webConfig the WebConfig
     * @param model the Model
     * @return a PathQuery
     */
    public static PathQuery makePathQueryForBag(InterMineBag imBag, WebConfig webConfig,
            Model model) {
        PathQuery query = new PathQuery(model);
        query.addViews(getAttributeViewForClass(imBag.getType(), model, webConfig));
        query.addConstraint(Constraints.in(imBag.getType(), imBag.getName()));
        return query;
    }

    /**
     * Create a PathQuery to get results for a collection of items from an InterMineObject
     *
     * @param webConfig the WebConfig
     * @param os the production ObjectStore
     * @param object the InterMineObject
     * @param referencedClassName the collection type
     * @param field the name of the field for the collection in the InterMineObject
     * @return a PathQuery
     */
    public static PathQuery makePathQueryForCollection(WebConfig webConfig, ObjectStore os,
            InterMineObject object,
            String referencedClassName, String field) {
        String className = TypeUtil.unqualifiedName(DynamicUtil.getSimpleClassName(object
                .getClass()));
        Path path;
        try {
            path = new Path(os.getModel(), className + "." + field);
        } catch (PathException e) {
            throw new IllegalArgumentException("Could not build path for \"" + className + "."
                    + field + "\".");
        }
        List<Class<?>> types = new ArrayList<Class<?>>();
        if (path.endIsCollection()) {
            types = queryForTypesInCollection(object, field, os);
            if (types.isEmpty()) {
                // the collection was empty, but still generate a query with the collection type
                types.add(os.getModel().getClassDescriptorByName(referencedClassName).getType());
            }
        } else if (path.endIsReference()) {
            types.add(path.getLastClassDescriptor().getType());
        }
        return makePathQueryForCollectionForClass(webConfig, os.getModel(), object, field, types);
    }

    /**
     * Search for the classes in a collection for a given InterMineObject, for example find all of
     * the sub-classes of Employee in the Department.employees collection of a given Department.
     * If there are no subclasses or the collection is empty a list with the type of the collection
     * is returned.
     * @param object an InterMineObject to inspect
     * @param field the name if the collection to check
     * @param os the ObjectStore in which to execute the query
     * @return a list of classes in the collection
     */
    public static List<Class<?>> queryForTypesInCollection(InterMineObject object, String field,
            ObjectStore os) {
        List<Class<?>> typesInCollection = new ArrayList<Class<?>>();

        // if there are no subclasses there can only be one type in the collection
        Model model = os.getModel();
        ClassDescriptor startCld =
            model.getClassDescriptorByName(DynamicUtil.getSimpleClassName(object));
        CollectionDescriptor col = startCld.getCollectionDescriptorByName(field, true);
        ClassDescriptor colCld = col.getReferencedClassDescriptor();

        if (model.getAllSubs(colCld).isEmpty()) {
            // there aren't any subclasses, so no need to do a query
            typesInCollection.add(colCld.getType());
        } else {
            // there may be multiple subclasses in the collection, need to run a query
            Query query = new Query();
            QueryClass qc = new QueryClass(colCld.getType());
            query.addFrom(qc);
            query.addToSelect(new QueryField(qc, "class"));
            query.setDistinct(true);
            query.setConstraint(new ContainsConstraint(new QueryCollectionReference(object, field),
                    ConstraintOp.CONTAINS, qc));
            for (Object o : os.executeSingleton(query)) {
                typesInCollection.add((Class<?>) o);
            }

            // Collection was empty but add collection type to be consistent with collection types
            // without subclasses.
            if (typesInCollection.isEmpty()) {
                typesInCollection.add(colCld.getType());
            }
        }
        return typesInCollection;
    }

    /**
     * Called by makePathQueryForCollection
     *
     * @param webConfig the webConfig
     * @param model the object model
     * @param object the InterMineObject
     * @param field the name of the field for the collection in the InterMineObject
     * @param sr the list of classes and subclasses
     * @return a PathQuery
     */
    private static PathQuery makePathQueryForCollectionForClass(WebConfig webConfig, Model model,
            InterMineObject object, String field, List<Class<?>> sr) {
        Class<?> commonClass = CollectionUtil.findCommonSuperclass(sr);
        String typeOfCollection =
            TypeUtil.unqualifiedName(DynamicUtil.getSimpleClassName(commonClass));
        String startClass = TypeUtil.unqualifiedName(DynamicUtil.getSimpleClassName(object
                .getClass()));
        String collectionPath = startClass + "." + field;
        PathQuery pathQuery = getQueryWithDefaultView(typeOfCollection, model, webConfig,
                collectionPath);
        pathQuery.addConstraint(Constraints.eq(startClass + ".id", object.getId().toString()));
        return pathQuery;
    }

    /**
     * Used for making a query for a reference or collection.  Only used when a user clicks on
     * [show all] under an inline table on an Object's report page.  The type of that object is
     * "startingPath", eg. Department.  This path will be prepended to every path in the query.
     * The "type" is the type of the reference/collection, eg. Employee.
     *
     * TODO use getDefaultViewForClass() instead
     *
     * @param objType class of object we are querying for eg. Manager
     * @param model the model
     * @param webConfig the webconfig
     * @param fieldType the type of the field this object is in, eg Employee
     * @return query, eg. Department.employees.name
     */
    protected static PathQuery getQueryWithDefaultView(String objType, Model model,
            WebConfig webConfig, String fieldType) {
        String prefix = fieldType;
        PathQuery query = new PathQuery(model);
        ClassDescriptor cld = model.getClassDescriptorByName(objType);
        List<FieldConfig> fieldConfigs = FieldConfigHelper.getClassFieldConfigs(webConfig, cld);

        if (!StringUtils.isBlank(prefix)) {
            try {
                // if the type is different to the end of the prefix path, add a subclass constraint
                Path fieldPath = new Path(model, fieldType);
                String fieldEndType = TypeUtil.unqualifiedName(fieldPath.getEndType().getName());
                if (!fieldEndType.equals(objType)) {
                    query.addConstraint(Constraints.type(fieldType, objType));
                }
            } catch (PathException e) {
                LOG.error("Invalid path configured in webconfig for class: " + objType);
            }
        }

        for (FieldConfig fieldConfig : fieldConfigs) {
            if (fieldConfig.getShowInResults()) {
                String path = prefix + "." + fieldConfig.getFieldExpr();
                int from = prefix.length() + 1;
                while (path.indexOf('.', from) != -1) {
                    int dotPos = path.indexOf('.', from);
                    int nextDot = path.indexOf('.', dotPos + 1);
                    String outerJoin = nextDot == -1 ? path.substring(0, dotPos)
                            : path.substring(0, nextDot);
                    query.setOuterJoinStatus(outerJoin, OuterJoinStatus.OUTER);
                    from = dotPos + 1;
                }
                query.addView(path);
            }
        }
        if (query.getView().size() == 0) {
            for (AttributeDescriptor att : cld.getAllAttributeDescriptors()) {
                if (!"id".equals(att.getName())) {
                    query.addView(prefix + "." + att.getName());
                }
            }
        }
        return query;
    }

    /**
     * Get the view for a path query reformatted to obey the labels given in webconfig.
     * So if Employee has the alias "Arbeitnehmer", department the alias "Abteilung", then
     * Employee.department.name would become "Arbeitnehmer > Abteilung > Name". Also,
     * camel-cased names will be decamelised, so "Contractor.oldCompanys.vatNumber" would become
     * "Contractor > Old Companys > Vat Number". ("VAT Number" can be achieved if that field is
     * labelled as such).
     *
     * @param pq The pathquery whose views to get.
     * @param webConfig The Web-Configuration
     *
     * @return A transformed list of strings.
     */
    public static List<String> getAliasedColumnHeaders(PathQuery pq, WebConfig webConfig) {
        List<String> views = pq.getView();
        List<String> aliasedViews = new ArrayList<String>();
        for (String view: views) {
            Path viewPath;
            try {
                viewPath = pq.makePath(view);
            } catch (PathException e) {
                throw new RuntimeException(e);
            }
            aliasedViews.add(WebUtil.formatPath(viewPath, webConfig));
        }

        return aliasedViews;
    }
}
