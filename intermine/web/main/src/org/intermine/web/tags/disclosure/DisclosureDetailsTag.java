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
 * This class renders disclosureDetails tag. See tag library descriptor for tag
 * description.
 * @author Jakub Kulaviak
 */
public class DisclosureDetailsTag extends BaseDisclosureTag
{
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
     * @throws JspException if element is not inside &lt;disclosure&gt; element
     * @throws IOException if error occurs during writing to stream output
     */
    public void doTag() throws JspException, IOException {
        DisclosureHeadTag parent =  (DisclosureHeadTag) getParent();
        if (parent == null) {
            throw new JspException("<disclosureDetails> element can be only inside "
                    + "<disclosureHead> element");
        }
        JspWriter out = getJspContext().getOut();
        out.write("<span");
        printStyleAndClass(out);
        out.write(">");
        getJspBody().invoke(null);
        out.write("</span>");
    }
}


