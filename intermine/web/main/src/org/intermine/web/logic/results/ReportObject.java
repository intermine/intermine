package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.map.LinkedMap;
import org.intermine.api.util.PathUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;

/**
 * Object to be displayed on report.do
 *
 * @author Radek Stepan
 * @author Richard Smith
 */
public class ReportObject
{
    private final InterMineObject object;
    private final WebConfig webConfig;
    @SuppressWarnings("unused")
    private final Map<String, String> webProperties;
    private final Model model;
    @SuppressWarnings("unused")
    private final Map<String, List<FieldDescriptor>> classKeys;
    private final String type;

    private Map<String, Object> fieldValues;

    /**
     * @List<ReportObjectField> setup list of summary fields this object has
     */
    private List<ReportObjectField> objectSummaryFields;

    /**
     * Setup internal DisplayObject
     * @param object InterMineObject
     * @param model Model
     * @param webConfig WebConfig
     * @param webProperties Map
     * @param classKeys List
     * @throws Exception Exception
     */
    @SuppressWarnings("unchecked")
    public ReportObject(InterMineObject object, Model model, WebConfig webConfig,
            Map webProperties, Map<String, List<FieldDescriptor>> classKeys) throws Exception {
        this.object = object;
        this.model = model;
        this.webConfig = webConfig;
        this.webProperties = webProperties;
        this.classKeys = classKeys;

        // infer dynamic type of IM object
        this.type = DynamicUtil.getSimpleClass(object).getName();
    }

    /**
     * Object field.
     *
     * @author Radek Stepan
     */
    public class ReportObjectField
    {
        /**
         * @String field name (key, expression)
         */
        private String fieldName;

        /**
         * @Object field value
         */
        private Object fieldValue;

        /**
         * @String link to a custom field displayer
         */
        private String fieldDisplayerPage;

        /**
         * @String path string (e.g. Gene.primaryIdentifier)
         */
        private String pathString;

        /**
         * Constructor
         * @objectType
         * @fieldName
         * @fieldValue
         * @fieldDisplayerPage
         */
        public ReportObjectField(String objectType, String fieldName,
                Object fieldValue, String fieldDisplayerPage) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.fieldDisplayerPage = fieldDisplayerPage;
            // form path string from an unqualified name and a field name
            this.pathString = TypeUtil.unqualifiedName(objectType) + "." + fieldName;
        }

        /**
         * Get field name
         * @return String
         */
        public String getName() {
            return fieldName;
        }

        /**
         * Get field value
         * @return Object
         */
        public Object getValue() {
            return fieldValue;
        }

        /**
         * Return path to a custom displayer
         * @return String
         */
        public String getDisplayerPage() {
            return fieldDisplayerPage;
        }

        /**
         * Does the field have a custom displayer defined for the value?
         * @return boolean
         */
        public boolean getValueHasDisplayer() {
            return fieldDisplayerPage != null;
        }

        /**
         * Get a path string to fetch field descriptions
         * @return String
         */
        public String getPathString() {
            return pathString;
        }

    }

    /**
     * A listing of object fields as pieced together from the various displayObject methods
     * @return <ReportObjectField>s List
     */
    public List<ReportObjectField> getObjectSummaryFields() {
        // are we setup yet?
        if (objectSummaryFields == null) {
            objectSummaryFields = new ArrayList<ReportObjectField>();

            // traverse all path expressions for the fields that should be used when
            //  summarising the object
            for (FieldConfig fc : getFieldConfigs()) {
                // get fieldName
                String fieldName = fc.getFieldExpr();

                // get fieldValue
                Object fieldValue = getFieldValue(fieldName);

                // get displayer
                //FieldConfig fieldConfig = fieldConfigMap.get(fieldName);
                String fieldDisplayer = fc.getDisplayer();

                // add new ReportObjectField
                objectSummaryFields.add(new ReportObjectField(
                        type,
                        fieldName,
                        fieldValue,
                        fieldDisplayer
                ));
            }
        }

        return objectSummaryFields;
    }

    /**
     * Get InterMine object
     * @return InterMineObject
     */
    public InterMineObject getObject() {
        return object;
    }

    /**
     * Return a list of field configs
     * @return Collection<FieldConfig>
     */
    private Collection<FieldConfig> getFieldConfigs() {
        Map<String, Type> types = webConfig.getTypes();
        if (types.containsKey(type)) {
            return types.get(type).getFieldConfigs();
        }
        return Collections.emptyList();
    }

    /**
     * Get field value for a field name (expression)
     * @param fieldExpression
     * @return Object
     */
    private Object getFieldValue(String fieldExpression) {
        // if field values as a whole are not set yet...
        if (fieldValues == null || fieldValues.isEmpty()) {
            // create a new map
            fieldValues = new HashMap<String, Object>();

            // fetch field configs
            for (FieldConfig fc : getFieldConfigs()) {
                // crete a path string
                String pathString = TypeUtil.unqualifiedName(type) + "." + fc.getFieldExpr();
                try {
                    // resolve path
                    Path path = new Path(model, pathString);
                    fieldValues.put(fc.getFieldExpr(), PathUtil.resolvePath(path, object));
                } catch (PathException e) {
                    throw new Error("There must be a bug", e);
                }
            }
        }

        // return a field value for a field expression (name)
        return fieldValues.get(fieldExpression);
    }

}
