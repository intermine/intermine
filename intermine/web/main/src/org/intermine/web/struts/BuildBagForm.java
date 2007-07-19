package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.struts.action.ActionForm;
import org.apache.struts.upload.FormFile;

/**
 * Form bean to represent the inputs needed to create a bag from user input.
 *
 * @author Kim Rutherford
 */

public class BuildBagForm extends ActionForm
{
    private String bagName;
    private FormFile formFile;
    private String text;
    private String type;
    private String extraFieldValue;
    private String whichInput;
    
    /**
     * Get the bag type
     * @return the bag type string
     */
    public String getType() {
        return type;
    }

    /**
     * Set the bag type
     * @param type the bag type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Set the query string
     * @param text the query string
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Get the text string
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

    /**
     * Get the bag name (existing bags)
     * @return the bag name
     */
    public String getBagName() {
        return bagName;
    }

    /**
     * Get the value to use when creating an extra constraint on a BagQuery, configured in
     * BagQueryConfig.
     * @return the extra field value
     */
    public String getExtraFieldValue() {
        return extraFieldValue;
    }

    /**
     * Set the extra field value.
     * @param extraFieldValue the extra field value
     */
    public void setExtraFieldValue(String extraFieldValue) {
        this.extraFieldValue = extraFieldValue;
    }
    
    /**
     * Set the method the user used to upload her bag.  
     * @param whichInput Which method the user used: paste or file
     */
    public void setWhichInput(String whichInput) {
        this.whichInput = whichInput;
    }

    /**
     * Get the method the user used to upload the bag.  Will be either 'paste' or 'file'.  Paste if
     * they typed in the entries via the form.  File if they uploaded a file from their computer.
     * @return which method the user used to upload her bag: paste or file
     */
    public String getWhichInput() {
        return whichInput;
    }

    
}
