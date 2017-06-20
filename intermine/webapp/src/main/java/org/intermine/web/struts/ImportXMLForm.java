package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.apache.struts.validator.ValidatorForm;

/**
 * Form bean representing a generic import form.
 *
 * @author Daniela Butano
 */
public class ImportXMLForm extends ValidatorForm
{
    protected String xml;
    protected FormFile formFile;

    /** Construct a form. **/
    public ImportXMLForm() {
        reset();
    }
    /**
     * Get the xml.
     * @return query in xml format
     */
    public String getXml() {
        return xml;
    }

    /**
     * Set the xml.
     * @param xml query in xml format
     */
    public void setXml(String xml) {
        this.xml = xml;
    }

    /**
     * Set the FormFile.
     * @param formFile the FormFile
     */
    public void setFormFile(FormFile formFile) {
        this.formFile = formFile;
    }

    /**
     * Get the FormFile.
     * @return the FormFile.
     */
    public FormFile getFormFile() {
        return formFile;
    }

    /**
     * Reset the form.
     */
    protected void reset() {
        xml = "";
        formFile = null;
    }

    /**
     * {@inheritDoc}
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        reset();
    }
}
