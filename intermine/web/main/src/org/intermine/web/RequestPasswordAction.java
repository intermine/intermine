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
import java.util.Random;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * Action to handle button presses RequestPasswordForm
 * 
 * @author Mark Woodbridge
 */
public class RequestPasswordAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(RequestPasswordAction.class);

    protected static Random random = new Random();

    /**
     * Method called when user has finished updating a constraint
     * 
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        Map webProperties = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String username = ((RequestPasswordForm) form).getUsername();

        boolean successful = false;
        if (pm.hasProfile(username)) {
            successful = email(username, pm.getPassword(username), webProperties);
        }
        if (successful) {
            recordMessage(new ActionMessage("login.emailed", username), request);
        } else {
            recordError(new ActionMessage("login.invalidemail"), request);
        }
        return mapping.findForward("login");
    }

    /**
     * Generate a random 8-letter String of lower-case characters
     * 
     * @return the String
     */
    public static String generatePassword() {
        String s = "";
        for (int i = 0; i < 8; i++) {
            s += (char) ('a' + random.nextInt(26));
        }
        return s;
    }

    /**
     * Email a password to an email address
     * 
     * @param to
     *            the address to send to
     * @param password
     *            the password to send
     * @param webProperties
     *            properties such as the from address
     * @return true if sending was successful
     */
    public static boolean email(String to, String password, Map webProperties) {
        try {
            String host = (String) webProperties.get("mail.host");
            String from = (String) webProperties.get("mail.from");
            String subject = (String) webProperties.get("mail.subject");
            String text = (String) webProperties.get("mail.text");
            text = MessageFormat.format(text, new Object[] { 
                       password });
            Properties properties = System.getProperties();
            properties.put("mail.smtp.host", host);
            MimeMessage message = new MimeMessage(Session.getDefaultInstance(properties, null));
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, InternetAddress.parse(to, true)[0]);
            message.setSubject(subject);
            message.setText(text);
            Transport.send(message);
            return true;
        } catch (Exception e) {
            LOG.warn(e);
            return false;
        }
    }
}
