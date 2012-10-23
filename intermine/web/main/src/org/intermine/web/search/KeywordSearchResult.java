package org.intermine.web.search;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.jfree.util.Log;

/**
 * Container for a single result row from the keyword search
 * @author nils
 */
public class KeywordSearchResult
{
    private static final Logger LOG = Logger.getLogger(KeywordSearchResult.class);

    final WebConfig webconfig;
    final InterMineObject object;

    final int id;
    final String type;
    final float score;
    final Map<String, TemplateQuery> templates;

    final int points;
    final HashMap<String, FieldConfig> fieldConfigs;
    final Vector<String> keyFields;
    final Vector<String> additionalFields;
    final HashMap<String, Object> fieldValues;

    /**
     * create the container object - automatically reads fields and saves the results in members
     * @param webconfig webconfig
     * @param object the object this result should contain
     * @param classKeys keys associated with this class
     * @param classDescriptor descriptor for this class
     * @param score score for this hit
     * @param templates templatequeries for this class
     */
    public KeywordSearchResult(WebConfig webconfig, InterMineObject object,
            Map<String, List<FieldDescriptor>> classKeys, ClassDescriptor classDescriptor,
            float score, Map<String, TemplateQuery> templates) {
        super();

        List<FieldConfig> fieldConfigList = FieldConfigHelper.getClassFieldConfigs(webconfig,
                classDescriptor);
        this.fieldConfigs = new HashMap<String, FieldConfig>();
        this.keyFields = new Vector<String>();
        this.additionalFields = new Vector<String>();
        this.fieldValues = new HashMap<String, Object>();

        for (FieldConfig fieldConfig : fieldConfigList) {
            if (fieldConfig.getShowInSummary()) {
                fieldConfigs.put(fieldConfig.getFieldExpr(), fieldConfig);

                if (ClassKeyHelper.isKeyField(classKeys, classDescriptor.getName(), fieldConfig
                        .getFieldExpr())) {
                    this.keyFields.add(fieldConfig.getFieldExpr());
                } else {
                    this.additionalFields.add(fieldConfig.getFieldExpr());
                }

                if (fieldConfig.getDisplayer() == null) {
                    Object value = getValueForField(object, fieldConfig.getFieldExpr());
                    if (value != null) {
                        fieldValues.put(fieldConfig.getFieldExpr(), value);
                    }
                }
            }
        }

        this.webconfig = webconfig;
        this.object = object;
        this.id = object.getId();
        this.type = classDescriptor.getUnqualifiedName();
        this.score = score;
        this.templates = templates;
        this.points = Math.round(Math.max(0.1F, Math.min(1, getScore())) * 10); // range 1..10
    }

    private Object getValueForField(InterMineObject object, String expression) {
        LOG.debug("Getting field " + object.getClass().getName() + " -> " + expression);
        Object value = null;

        try {
            int dot = expression.indexOf('.');
            if (dot > -1) {
                String subExpression = expression.substring(dot + 1);
                Object reference = object.getFieldValue(expression.substring(0, dot));
                LOG.debug("Reference=" + reference);

                // recurse into next object
                if (reference != null) {
                    if (reference instanceof InterMineObject) {
                        value = getValueForField((InterMineObject) reference, subExpression);
                    } else {
                        LOG.warn("Reference is not an IMO in " + object.getClass().getName()
                                + " -> " + expression);
                    }
                }
            } else {
                value = object.getFieldValue(expression);
            }
        } catch (Exception e) {
            Log.error("Value/reference not found", e);
        }
        return value;
    }

    /**
     * returns original intermine object
     * @return object
     */
    public InterMineObject getObject() {
        return object;
    }

    /**
     * intermine ID
     * @return x
     */
    public int getId() {
        return id;
    }

    /**
     * returns the name of the class for this object (category)
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * return score
     * @return ...
     */
    public float getScore() {
        return score;
    }

    /**
     * templates associated with this class
     * @return map of internal template name to template query
     */
    public Map<String, TemplateQuery> getTemplates() {
        return templates;
    }

    /**
     * return points
     * @return 1..10
     */
    public int getPoints() {
        return points;
    }

    /**
     * fieldConfigs
     * @return map from field expression to fieldConfigs
     */
    public HashMap<String, FieldConfig> getFieldConfigs() {
        return fieldConfigs;
    }

    /**
     * key field expressions
     * @return keyFields
     */
    public final Vector<String> getKeyFields() {
        return keyFields;
    }

    /**
     * additional display field expressions
     * @return additionalFields
     */
    public final Vector<String> getAdditionalFields() {
        return additionalFields;
    }

    /**
     * values of all fields
     * @return map from field expression to value
     */
    public HashMap<String, Object> getFieldValues() {
        return fieldValues;
    }

}
