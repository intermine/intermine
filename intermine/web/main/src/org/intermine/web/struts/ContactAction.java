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

import java.text.MessageFormat;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.MessageResources;
import org.intermine.web.logic.session.SessionMethods;
import javax.mail.Session;
/**
 * Action handles submission of user feedback form.
 *
 * @author Thomas Riley
 */
public class ContactAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(ContactAction.class);

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
    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ContactForm ff = (ContactForm) form;
        try {
            Properties webProperties = SessionMethods.getWebProperties(session.getServletContext());
            MessageResources strings = getResources(request);

            final String user = webProperties.getProperty("mail.smtp.user");
            String smtpPort = webProperties.getProperty("mail.smtp.port");
            String authFlag = webProperties.getProperty("mail.smtp.auth");
            String starttlsFlag = webProperties.getProperty("mail.smtp.starttls.enable");

            String host = webProperties.getProperty("mail.host");
            String from = ff.getMonkey();
            String subject = ff.getSubject();
            String text = MessageFormat.format(strings.getMessage("contact.template"),
                                new Object[] {ff.getName(), ff.getMonkey(), ff.getMessage()});
            String dest = webProperties.getProperty("feedback.destination");
            Properties properties = System.getProperties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.user", user);
            // Fix to "javax.mail.MessagingException: 501 Syntactically
            // invalid HELO argument(s)" problem
            // See http://forum.java.sun.com/thread.jspa?threadID=487000&messageID=2280968
            properties.put("mail.smtp.localhost", "localhost");
            if (smtpPort != null) {
                properties.put("mail.smtp.port", smtpPort);
            }

            if (starttlsFlag != null) {
                properties.put("mail.smtp.starttls.enable", starttlsFlag);
            }
            if (authFlag != null) {
                properties.put("mail.smtp.auth", authFlag);
            }
            Session mailSession;
            if (authFlag != null && ("true".equals(authFlag) || "t".equals(authFlag))) {
                Authenticator authenticator = new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        String password = (String) webProperties.get("mail.server.password");
                        return new PasswordAuthentication(user, password);
                    }
                };
                mailSession = Session.getDefaultInstance(properties, authenticator);
            } else {
                mailSession = Session.getDefaultInstance(properties);
            }
            MimeMessage message = new MimeMessage(mailSession);
            message.setReplyTo(InternetAddress.parse(from, true));
            message.setFrom(new InternetAddress(user));
            message.addRecipient(Message.RecipientType.TO, InternetAddress.parse(dest, true)[0]);
            message.setSubject(subject);
            message.setText(text);
            Transport.send(message);

            recordMessage(new ActionMessage("contact.sent"), request);

            // avoid showing form
            request.setAttribute("sent", Boolean.TRUE);
            ff.reset(mapping, request); // clear bean (we don't clear it if an error occurs)

        } catch (Exception e) {
            request.setAttribute("response", e);
            recordError(new ActionMessage("contact.failed", e), request, e, LOG);
        }

        return mapping.findForward("contact");
    }

}
