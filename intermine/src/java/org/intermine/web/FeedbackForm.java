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

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

/**
 * Form bean representing feedback form.
 *
 * @author  Thomas Riley
 */
public class FeedbackForm extends ValidatorForm {
    private String name;
    private String email;
    private String subject;
    private String message;
    
    /** Creates a new instance of FeedbackForm */
    public FeedbackForm() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name=name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email=email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject=subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message=message;
    }

    /**
     * WHen there are no other errors, check email address is valid.
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {

        ActionErrors errors = super.validate(mapping, request);
        
        if ((errors == null || errors.size() == 0) &&getEmail().indexOf('@') == -1)
        {
            if (errors == null)
                errors = new ActionErrors();
            
            errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionError("errors.email", getEmail()));
        }
        
        return errors;
    }
}
