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
import java.util.List;
import java.util.Map;

import org.intermine.api.util.PathUtil;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.FieldConfig;
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
     * @param classKeys List
     * @throws Exception Exception
     */
    public ReportObject(InterMineObject object, Model model, WebConfig webConfig,
            Map<String, List<FieldDescriptor>> classKeys) throws Exception {
        this.object = object;
        this.model = model;
        this.webConfig = webConfig;
        this.classKeys = classKeys;

        // infer dynamic type of IM object
        this.type = DynamicUtil.getSimpleClass(object).getName();
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
                // show in summary...
                if (fc.getShowInSummary()) {
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
    protected Collection<FieldConfig> getFieldConfigs() {
        Map<String, Type> types = webConfig.getTypes();
        if (types.containsKey(type)) {
            return types.get(type).getFieldConfigs();
        }
        return Collections.emptyList();
    }

    /**
     * Get field value for a field name (expression)
     * @param fieldExpression String
     * @return Object
     */
    protected Object getFieldValue(String fieldExpression) {
        // if field values as a whole are not set yet...
        if (fieldValues == null) {
            setupFieldValues();
        }
        // return a field value for a field expression (name)
        return fieldValues.get(fieldExpression);
    }

    /**
     * Setup fieldValues HashMap
     */
    protected void setupFieldValues() {
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

}
