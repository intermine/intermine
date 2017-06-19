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

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * Base tag handler from which are all disclosure tag handlers derived.
 * @author Jakub Kulaviak
 */
public class BaseDisclosureTag extends SimpleTagSupport
{

    private String style;
    private String styleClass;

    /**
     * Returns element style.
     * @return element style
     */
    public String getStyle() {
        return style;
    }

    /**
     * Sets element style.
     * @param style the style
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Returns css style class of element.
     * @return css class
     */
    public String getStyleClass() {
        return styleClass;
    }

    /**
     * Sets css style class of element.
     * @param styleClass the style class
     */
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    /**
     * Prints 'style' and 'class' attributes of element.
     * @param out output
     * @throws IOException if exception occurs during writing to output stream
     */
    protected void printStyleAndClass(JspWriter out) throws IOException {
        if (getStyleClass() != null || getDefaultStyleClass() != null) {
            out.write(" class=\"");
            if (getStyleClass() != null) {
                out.write(getStyleClass());
            } else {
                out.write(getDefaultStyleClass());
            }
            out.write("\" ");
        }
        if (getStyle() != null) {
            out.write(" style=\"");
            out.write(getStyle());
            out.write("\" ");
        }
        out.write("");
    }

    /**
     * Returns default style class. Each of tags has its own default style class. So
     * if you want to set style of disclosure tag, just simply set 'disclosure' class
     * in style sheet  file.
     * @return default style class
     */
    protected String getDefaultStyleClass() {
        return null;
    }
}
