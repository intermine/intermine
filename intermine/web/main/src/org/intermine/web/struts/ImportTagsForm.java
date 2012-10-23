package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Form bean representing tags import form.
 *
 * @author  Thomas Riley
 */
public class ImportTagsForm extends ActionForm
{
    private String xml;
    private boolean overwriting = false;

    /**
     * Creates a new instance of ImportTagsForm.
     */
    public ImportTagsForm() {
        reset();
    }

    /**
     * Get the xml.
     * @return tags in xml format
     */
    public String getXml() {
        return xml;
    }

    /**
     * Set the xml.
     * @param xml tags in xml format
     */
    public void setXml(String xml) {
        this.xml = xml;
    }

    /**
     * Get the overwrite flag.
     * @return  true to overwrite existing template, false to add
     */
    public boolean isOverwriting() {
        return overwriting;
    }

    /**
     * Set the overwriting flag.
     * @param overwriting true to overwrite existing templates, false to add
     */
    public void setOverwriting(boolean overwriting) {
        this.overwriting = overwriting;
    }


    /**
     * {@inheritDoc}
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        reset();
    }

    /**
     * Reset the form.
     */
    protected void reset() {
        xml = "";
        overwriting = false;
    }
}
