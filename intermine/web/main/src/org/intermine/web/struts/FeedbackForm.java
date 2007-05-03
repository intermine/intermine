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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.PathQueryBinding;

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
     * When there are no other errors, check email address is valid. Check for headers
     * being injected into the message or subject.
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
        
        StringBuffer buffer = new StringBuffer();
        boolean badMessage = false;
        
        if (containsHeaders(getMessage(), buffer)) {
            message = buffer.toString();
            badMessage = true;
        }
        
        buffer = new StringBuffer();
        if (containsHeaders(getEmail(), buffer)) {
            email = buffer.toString();
            badMessage = true;
        }
        
        buffer = new StringBuffer();
        if (containsHeaders(getSubject(), buffer)) {
            subject = buffer.toString();
            badMessage = true;
        }
        
        buffer = new StringBuffer();
        if (containsHeaders(getName(), buffer)) {
            name = buffer.toString();
            badMessage = true;
        }
        
        if (badMessage) {
            if (errors == null) {
                errors = new ActionErrors();
            }
            errors.add(ActionErrors.GLOBAL_MESSAGE,
                    new ActionMessage("errors.feedback.invalidmessage"));
        }
        
        return errors;
    }
    
    /**
     * Check part of the message for injected headers.
     * @param message some part of the message
     * @param buffer output buffer with bad lines stripped
     * @return true if message contains headers, false if not
     */
    protected boolean containsHeaders(String message, StringBuffer buffer) {
        String lines[] = message.split("\n");
        boolean found = false;
        for (int i = 0; i < lines.length; i++) {
            if (isEmailHeader(lines[i], "to")
                || isEmailHeader(lines[i], "from")
                || isEmailHeader(lines[i], "bcc")
                || isEmailHeader(lines[i], "cc")) {
                found = true;
            } else {
                buffer.append(lines[i]);
                if (i < lines.length - 1) {
                    buffer.append("\n");
                }
            }
        }
        return found;
    }
    
    private boolean isEmailHeader(String line, String header) {
        return (line.toLowerCase().startsWith(header + ":") && line.indexOf("@") >= 0);
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
        message = createDefaultFeedbackMsg(request);
        email = "";
        
        Profile profile = (Profile) request.getSession().getAttribute(Constants.PROFILE);
        if (profile != null) {
            email = profile.getUsername();
        }
    }
    
    /**
     * Create the default feedback messages. Adds URL and current query by default.
     * 
     * @param request current http request
     * @return default feedback message
     */
    protected String createDefaultFeedbackMsg(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String msg = "\n\n\n\n---- Current page: ----\n\n";
        msg += request.getAttribute("javax.servlet.forward.servlet_path");
        if (request.getQueryString() != null) {
            msg += "?" + request.getQueryString();
        }
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        if (query != null) {
            msg += "\n\n---- Current query: ----\n\n";
            msg += PathQueryBinding.marshal(query, "", query.getModel().getName());
        }
        return msg;
    }
}
