package org.intermine.web.tags.disclosure;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

/**
 * This class renders disclosureHead tag. See tag library descriptor for tag description.
 * 
 */

public class DisclosureHeadTag extends BaseDisclosureTag {

	private static final String DEFAULT_STYLE_CLASS = "disclosureHead";
	private DisclosureTag parent;

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
		parent = (DisclosureTag) getParent();
		if (parent == null) {
			throw new JspException(
					"<disclosureHead> element can be only inside <disclosure> element");
		}
		JspWriter out = getJspContext().getOut();

		out.write("<div");
		printStyleAndClass(out);
		out.write("><a href=\"");
		out.write(getLink());
		out.write("\">");
		
		String imgSrc;
		if (parent.getOpened()) {
			imgSrc = "images/disclosed.gif";
		} else {
			imgSrc = "images/undisclosed.gif";
		}
		out.write("<img border=\"0\" src=\"" + imgSrc + "\" alt=\"-\" id=\""
				+ parent.getId() + "Toggle\" />");
		out.write("</a>");
		getJspBody().invoke(null);
		out.write("</div>");
	}

	/**
	 * @see DisclosureTag.getLink()
	 */
	public String getLink() {
		return parent.getLink();
	}
}
