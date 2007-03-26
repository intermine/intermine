package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.MessageResources;

import org.apache.log4j.Logger;

/**
 * Action handles submission of user feedback form.
 *
 * @author Thomas Riley
 */
public class FeedbackAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(FeedbackAction.class);
    
    /** 
     * Method called when user has submitted valid feedback form
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        FeedbackForm ff = (FeedbackForm) form;
        
        try {
            Map webProperties = (Map) session.getServletContext()
                .getAttribute(Constants.WEB_PROPERTIES);
            MessageResources strings = getResources(request);
            String host = (String) ((Map) session.getServletContext().
                        getAttribute(Constants.WEB_PROPERTIES)).get("mail.host");
            String from = ff.getEmail();
            String subject = ff.getSubject();
            String text = MessageFormat.format(strings.getMessage("feedback.template"),
                                new Object[] {ff.getName(), ff.getEmail(), ff.getMessage()});
            String dest = (String) webProperties.get("feedback.destination");
            Properties properties = System.getProperties();
            properties.put("mail.smtp.host", host);
            
            MimeMessage message = new MimeMessage(Session.getDefaultInstance(properties, null));
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, InternetAddress.parse(dest, true)[0]);
            message.setSubject(subject);
            message.setText(text);
            Transport.send(message);
            
            recordMessage(new ActionMessage("feedback.sent"), request);
            
            // avoid showing form
            request.setAttribute("sent", Boolean.TRUE);
            ff.reset(mapping, request); // clear bean (we don't clear it if an error occurs)
        
        } catch (Exception e) {
            recordError(new ActionMessage("feedback.failed", e), request, e, LOG);
        }

        return mapping.findForward("feedback");
    }

}
