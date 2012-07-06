package org.intermine.web.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class EditPropertiesForm extends ActionForm
{
	private String propertyName = null;
	private String propertyValue = null;
	
	public String getPropertyName() {
		return propertyName;
	}
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
	public String getPropertyValue() {
		return propertyValue;
	}
	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	@Override
	public void reset(
			ActionMapping mapping,
			HttpServletRequest request
			) {
		propertyName = null;
		propertyValue = null;
	}
}
