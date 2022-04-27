package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.util.MailUtils;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ServiceException;
import java.text.MessageFormat;

/**
 * Send feedback from users
 * @author Daniela Butano
 *
 */
public class FeedbackService extends JSONService
{
    private static final Logger LOG = Logger.getLogger(FeedbackService.class);

    /**
     * Constructor
     * @param im A reference to the InterMine API settings bundle
     */
    public FeedbackService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String from = getOptionalParameter("email");
        String feedback = getRequiredParameter("feedback");
        String destination = webProperties.getProperty("feedback.destination");

        String body = null;
        if (from != null) {
            String feedbackMailText = "Email: {0}\n\n{1}";
            body = MessageFormat.format(feedbackMailText, new Object[] {from, feedback});
        } else {
            body = feedback;
        }
        try {
            MailUtils.email(destination, "Feedback", body, from, webProperties);
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            throw new ServiceException("Error sending feedback.");
        }
    }
}
