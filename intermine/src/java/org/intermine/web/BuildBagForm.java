package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.struts.upload.FormFile;
import org.apache.struts.action.ActionForm;

/**
 * Form bean to represent the inputs needed to create a bag from user input.
 *
 * @author Kim Rutherford
 */

public class BuildBagForm extends ActionForm
{
    protected FormFile formFile;
    protected String text;

    /**
     * Set the query string
     *
     * @param text the query string
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Get the text string
     *
     * @return the text string
     */
    public String getText() {
        return text;
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
}
