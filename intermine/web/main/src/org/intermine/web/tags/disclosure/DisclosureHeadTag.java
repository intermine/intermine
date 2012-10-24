package org.intermine.web.tags.disclosure;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

/**
 * This class renders disclosureHead tag. See tag library descriptor for tag description.
 * @author Jakub Kulaviak
 */
public class DisclosureHeadTag extends BaseDisclosureTag
{
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
     * @throws JspException if element is not inside &lt;disclosure&gt; element
     * @throws IOException if error occurs during writing to stream output
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
     * @return link
     */
    public String getLink() {
        return parent.getLink();
    }
}
