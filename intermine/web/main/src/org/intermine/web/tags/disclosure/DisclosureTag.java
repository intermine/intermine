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
import javax.servlet.jsp.PageContext;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.results.WebState;

/**
 * This class renders disclosure tag. See tag library descriptor for tag description.
 * @author Jakub Kulaviak
 */
public class DisclosureTag extends BaseDisclosureTag
{
    private static final String DEFAULT_STYLE_CLASS = "disclosure";
    private static final String CONSISTENT = "consistent";
    private String id;
    private boolean opened = true;
    private String onClick;
    private String type = "simple";

    /**
     * Returns type of tag. At this moment is relevant only 'consistent' type.
     * @return type of tag
     */
    public String getType() {
        return type.toLowerCase();
    }

    /**
     * Sets type of tag.
     * @param type type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultStyleClass() {
        return DEFAULT_STYLE_CLASS;
    }

    /**
     * @return additional javascript code, that should be executed on element change.
     */
    public String getOnClick() {
        return onClick;
    }

    /**
     * @param onChange additional javascript code, that should be executed on element change.
     */
    public void setOnClick(String onChange) {
        this.onClick = onChange;
    }

    /**
     * Sets element id. Disclosure tag is implemented with div, it sets div id.
     * @param id element id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets element id.
     * @return element id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns true if disclosure is opened else false.
     * @return disclosure state
     */
    public boolean getOpened() {
        if (isConsistentType()) {
            // Alas, we do not have access to the session, so we can't use SessionMethods
            WebState webState = (WebState) getJspContext().getAttribute(Constants.WEB_STATE,
                    PageContext.SESSION_SCOPE);
            if (webState != null) {
                Boolean ret = webState.getToggledElements().get(getId());
                if (ret != null) {
                    return ret;
                }
            }
        }
        return opened;
    }

    /**
     * @return true if disclosure is consistent type, i.e. saves its state (opened or closed)
     * during user session
     */
    public boolean isConsistentType() {
        return getType().equals(CONSISTENT);
    }

    /**
     * Sets new state of disclosure.
     * @param opened true if should be opened
     */
    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    /**
     * Renders tag.
     * @throws IOException if error occurs during writing to stream output
     * @throws JspException if JspException error occurs during rendering nested tags
     */
    public void doTag() throws JspException, IOException {
        JspWriter out = getJspContext().getOut();
        out.write("<div");
        printStyleAndClass(out);
        out.write(">");
        getJspBody().invoke(null);
        // It is displayed opened and hidden (if specified) with javascript ->
        // Client browser without javascript doesn't hide the content and user can see it
        // Else he wouldn't have possibility to see the content
        if (!getOpened()) {
            printJavascriptHides(out);
        }
        out.write("</div>");
    }

    private void printJavascriptHides(JspWriter out) throws IOException {
        out.write("<script type=\"text/javascript\">toggleHidden(\'");
        out.write(getId());
        out.write("\')</script>");
    }

    /**
     * Returns link switching between displayed and hidden state.
     * @return link
     */
    public String getLink() {
        StringBuilder sb = new StringBuilder();
        sb.append("javascript:toggleHidden(\'");
        sb.append(getId());
        sb.append("\');");
        if (isConsistentType()) {
            sb.append("saveToggleState(\'");
            sb.append(getId());
            sb.append("\');");
        }
        if (getOnClick() != null) {
            sb.append(getOnClick());
            sb.append(";");
        }
        return sb.toString();
    }
}
