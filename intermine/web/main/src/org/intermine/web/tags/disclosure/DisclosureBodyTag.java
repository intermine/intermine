package org.intermine.web.tags.disclosure;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;


/**
 * This class renders disclosureBody tag. See tag library descriptor for tag
 * description.
 * 
 */
public class DisclosureBodyTag extends BaseDisclosureTag {
	
	private static final String DEFAULT_STYLE_CLASS = "disclosureBody";

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
		JspWriter out = getJspContext().getOut();
		DisclosureTag parent = (DisclosureTag) getParent();
		if (parent == null) {
			throw new JspException(
					"<disclosureBody> element can be only inside <disclosure> element");
		}

		out.write("<div ");
		printStyleAndClass(out);
		out.write(">");

		{
			// Nested div
			out.write("<div id=\"");
			out.write(parent.getId());
			out.write("\" style=\"display: block\">");
			getJspBody().invoke(null);
			out.write("</div>");
		}

		out.write("</div>");
	}
}

