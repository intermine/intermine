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
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;

/**
 * A wrapper for DisplayObject.
 *
 * @author Radek Stepan
 */
public class ReportObject
{
    /**
     * @DisplayObject internal object to keep phasing out
     */
    private DisplayObject displayObject;

    /**
     * @List<ReportObjectField> setup list of summary fields this object has
     */
    private List<ReportObjectField> objectSummaryFields;

    /**
     * @Map<String, String> class descriptions passed from Controller
     */
    private Map<String, String> classDescriptions;

    /**
     * Setup internal DisplayObject
     * @param object InterMineObject
     * @param model Model
     * @param webConfig WebConfig
     * @param webProperties Map
     * @param classKeys List
     * @throws Exception Exception
     */
    public ReportObject(InterMineObject object, Model model, WebConfig webConfig,
            Map webProperties, Map<String, List<FieldDescriptor>> classKeys) throws Exception {
        displayObject = new DisplayObject(object, model, webConfig, webProperties, classKeys); // XXX: deprecate
    }

    /**
     * Object field.
     *
     * @author Radek Stepan
     */
    public class ReportObjectField
    {

        private String fieldName;
        private Object fieldValue;
        private String fieldDisplayerPage;
        private String fieldDescription;

        public ReportObjectField(String fieldName, Object fieldValue, String fieldDisplayerPage, String fieldDescription) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.fieldDisplayerPage = fieldDisplayerPage;
            this.fieldDescription = fieldDescription;
        }

        public String getName() {
            return fieldName;
        }

        public Object getValue() {
            return fieldValue;
        }

        public String getDisplayerPage() {
            return fieldDisplayerPage;
        }

        public boolean getValueHasDisplayer() {
            return fieldDisplayerPage != null;
        }

        public String getDescription() {
            return fieldDescription;
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

            // fetch FieldConfig map (displayers)
            Map<String, FieldConfig> fieldConfigMap =
                displayObject.getFieldConfigMap(); // XXX: deprecate

            // field values
            displayObject.getFieldExprs(); // XXX: deprecate
            Map<String, Object> fieldValues = displayObject.getFieldValues(); // XXX: deprecate

            // fetch class descriptor set
            Set<ClassDescriptor> classDescriptorSet = displayObject.getClds(); // XXX: deprecate
            // throw exception if classDescriptions are not set from Controller
            if (classDescriptions == null) {
                // blee
            }

            // traverse all path expressions for the fields that should be used when
            //  summarising the object
            for (
                    Iterator<String> i = displayObject.getFieldConfigMap().keySet().iterator();
                    i.hasNext();) { // XXX: deprecate
                // get fieldName
                String fieldName = i.next();

                // get fieldValue
                Object fieldValue = (fieldValues != null)
                    ? fieldValues.get(fieldName) : "";

                // get displayer
                FieldConfig fieldConfig = fieldConfigMap.get(fieldName);
                String fieldDisplayer = fieldConfig.getDisplayer();

                // field descriptions
                String fieldDescription = null;
                for (ClassDescriptor classDescriptor : classDescriptorSet) {
                    // get object class name
                    if (classDescriptor.getUnqualifiedName() != null) {
                        // attach a field name
                        String fieldDescriptionKey = classDescriptor.getUnqualifiedName() + "." + fieldName;
                        // get the actual description
                        fieldDescription += classDescriptions.get(fieldDescriptionKey);
                    }
                }

                // add new ReportObjectField
                objectSummaryFields.add(new ReportObjectField(
                        fieldName,
                        fieldValue,
                        fieldDisplayer,
                        fieldDescription
                ));
            }
        }

        return objectSummaryFields;
    }

    public InterMineObject getObject() {
        return displayObject.getObject(); // XXX: deprecate
    }

    public Set<ClassDescriptor> getClds() {
        return displayObject.getClds(); // XXX: deprecate
    }

    public void setClassDescriptions(Map<String, String> classDescriptions) {
        this.classDescriptions = classDescriptions; // FIXME: ideally, we would not have to make a call from a Controller
    }

}
