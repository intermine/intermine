package org.flymine.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.jfree.util.Log;

/**
 * Container for a single result row from the keyword search
 * @author nils
 *
 */
public class KeywordSearchResult {
    private static final Logger LOG = Logger.getLogger(KeywordSearchResult.class);
    
	final WebConfig webconfig;
	final InterMineObject object;
	
	final int id;
	final String type;
	final float score;
	final int points;
	
	final HashMap<String, FieldConfig> fieldConfigs;
	final Vector<String> keyFields;
	final Vector<String> additionalFields;
	final HashMap<String, Object> fieldValues;
	
	public KeywordSearchResult(WebConfig webconfig,
			InterMineObject object,
			Map<String, List<FieldDescriptor>> classKeys,
			ClassDescriptor classDescriptor, float score) {
		super();
		
		List<FieldConfig> fieldConfigList = FieldConfigHelper.getClassFieldConfigs(webconfig, classDescriptor);
		this.fieldConfigs = new HashMap<String, FieldConfig>();
		this.keyFields = new Vector<String>();
		this.additionalFields = new Vector<String>();
		this.fieldValues = new HashMap<String, Object>();	
		
		for (FieldConfig fieldConfig : fieldConfigList) {
		    if(fieldConfig.getShowInSummary()) {
    		    fieldConfigs.put(fieldConfig.getFieldExpr(), fieldConfig);
    		    
    			if(ClassKeyHelper.isKeyField(classKeys, classDescriptor.getName(), fieldConfig.getFieldExpr())) {
    				this.keyFields.add(fieldConfig.getFieldExpr());
    			} else {
    				this.additionalFields.add(fieldConfig.getFieldExpr());
    			}
    			
    			if(fieldConfig.getDisplayer() == null)
    			{
    				fieldValues.put(fieldConfig.getFieldExpr(), getValueForField(object, fieldConfig.getFieldExpr()));
    			}
		    }
		}

		this.webconfig = webconfig;
		this.object = object;
		this.id = object.getId();
		this.type = classDescriptor.getUnqualifiedName();
		this.score = score;
		this.points = Math.round(Math.max(0.1F, Math.min(1, getScore())) * 10); // range 1..10
	}
	
	private Object getValueForField(InterMineObject object, String expression) {
		LOG.debug("Getting field " + object.getClass().getName() + " -> " + expression);
		Object value = null;
		
		try {
			int dot = expression.indexOf('.');
			if(dot > -1) {
				String subExpression = expression.substring(dot + 1);
				InterMineObject reference = (InterMineObject)TypeUtil.getFieldValue(object, expression.substring(0, dot));
				LOG.debug("Reference=" + reference);
				
				//recurse into next object
				if(reference != null) {
					value = getValueForField(reference, subExpression);
				}
			} else {
				value = TypeUtil.getFieldValue(object, expression);
			}
		} catch (IllegalAccessException e) {
			Log.warn(null, e);
		}
		
		return value;
	}

	public InterMineObject getObject() {
		return object;
	}
	
	public int getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public float getScore() {
		return score;
	}
	
	public int getPoints() {
		return points;
	}

	public WebConfig getWebconfig() {
		return webconfig;
	}

	public HashMap<String, FieldConfig> getFieldConfigs() {
		return fieldConfigs;
	}

	public final Vector<String> getKeyFields() {
		return keyFields;
	}

	public final Vector<String> getAdditionalFields() {
		return additionalFields;
	}

	public HashMap<String, Object> getFieldValues() {
		return fieldValues;
	}
	
}
