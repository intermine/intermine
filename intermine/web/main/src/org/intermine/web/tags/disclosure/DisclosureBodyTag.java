package org.intermine.web.tags.disclosure;

/*
 * Copyright (C) 2002-2016 FlyMine
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
 * This class renders disclosureBody tag. See tag library descriptor for tag
 * description.
 * @author Jakub Kulaviak
 */
public class DisclosureBodyTag extends BaseDisclosureTag
{
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
     * @throws JspException if element is not inside &lt;disclosure&gt; element
     * @throws IOException if error occurs during writing to stream output
     */
    @Override
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

