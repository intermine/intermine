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
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

/**
 * Form bean representing feedback form.
 *
 * @author  Thomas Riley
 */
public class FeedbackForm extends ValidatorForm
{
    private String name;
    private String email;
    private String subject;
    private String message;
    
    /** Creates a new instance of FeedbackForm */
    public FeedbackForm() {
    }
    
    /**
     * @return name of person sending feedback
     */
    public String getName() {
        return name;
    }

    /**
     * @param name name of sender
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @return email address of sender
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email address of sender
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return feedback subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @param subject subject of feedback
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * @return feedback message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message feedback message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * WHen there are no other errors, check email address is valid.
     *
     * @param mapping ActionMapping of current action
     * @param request current servlet request
     * @return validation errors
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {

        ActionErrors errors = super.validate(mapping, request);
        
        if ((errors == null || errors.size() == 0) && getEmail().indexOf('@') == -1) {
            if (errors == null) {
                errors = new ActionErrors();
            }
            
            errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("errors.email", getEmail()));
        }
        
        return errors;
    }

    /**
     * Reset form bean. If user is logged in then the <code>email</code>
     * property is set to the profile username.
     *
     * @param mapping  the action mapping associated with this form bean
     * @param request  the current http servlet request
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        
        name = "";
        subject = "";
        message = "";
        email = "";
        
        Profile profile = (Profile) request.getSession().getAttribute(Constants.PROFILE);
        if (profile != null) {
            email = profile.getUsername();
        }
    }
    
    
}
