package org.intermine.web.tags.util;

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
import java.io.StringWriter;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;


/**
 * Tag escaping apostrophes, quotes and replacing "\n" with html tag for end of line.
 *
 * @author Jakub Kulaviak
 **/
public class TreatStringTag extends SimpleTagSupport
{
    /**
     * {@inheritDoc}
     */
    public void doTag() throws JspException, IOException {
        JspWriter out = getJspContext().getOut();
        StringWriter buffer = new StringWriter();
        getJspBody().invoke(buffer);
        String msg = buffer.toString();
        out.print(treatMessage(msg));
    }

    private String treatMessage(String msg) {
        msg = msg.replaceAll("\\\'", "\\\\'");
        msg = msg.replaceAll("\\\"", "&quot;");
        msg = msg.replaceAll("\\\\n", "<br />");
        return msg;
    }
}
