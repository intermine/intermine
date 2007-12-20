package org.intermine.web.tags.disclosure;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

/**
 * This class renders disclosureTitle tag. See tag library descriptor for tag
 * description.
 * 
 */

public class DisclosureTitleTag extends BaseDisclosureTag {

	private static final String DEFAULT_STYLE_CLASS = "disclosureTitle";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getDefaultStyleClass() {
		return DEFAULT_STYLE_CLASS;
	}

	/**
	 * Renders tag.
	 */
	public void doTag() throws JspException, IOException {
		DisclosureHeadTag parent = (DisclosureHeadTag) getParent();
		if (parent == null) {
			throw new JspException(
					"<disclosureTitle> element can be only inside <disclosureHead> element");
		}
		JspWriter out = getJspContext().getOut();
		out.write("<span");
		printStyleAndClass(out);
		out.write(">");
		out.write("<a href=\"");
		out.write(parent.getLink());
		out.write("\">");
		getJspBody().invoke(null);
		out.write("</a></span>");
	}
}
