package org.intermine.web.tags.disclosure;
import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

/**
 * This class renders disclosureDetails tag. See tag library descriptor for tag
 * description.
 * 
 */

public class DisclosureDetailsTag extends BaseDisclosureTag { 
   	
	private static final String DEFAULT_STYLE_CLASS = "disclosureDetails";

	/**
	 * {@inheritDoc}}
	 */
	@Override
	protected String getDefaultStyleClass() {
		return DEFAULT_STYLE_CLASS;
	}	
	
	/**
	 * Renders tag.
	 */
   public void doTag() throws JspException, IOException {
	   DisclosureHeadTag parent =  (DisclosureHeadTag) getParent();
	   if (parent == null) {
		   throw new JspException("<disclosureDetails> element can be only inside <disclosureHead> element");
	   }
	   JspWriter out = getJspContext().getOut();
	   out.write("<span");
	   printStyleAndClass(out);
	   out.write(">");
	   getJspBody().invoke(null);
	   out.write("</span>");
   }      
}


