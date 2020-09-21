package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2020 FlyMine
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
import org.intermine.util.PropertiesUtil;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ServiceException;

import java.text.MessageFormat;
import java.util.Properties;

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
     */
    public FeedbackService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String from = getOptionalParameter("email");
        String feedback = getRequiredParameter("feedback");
        Properties props = PropertiesUtil.getProperties();
        String destination = props.getProperty("feedback.destination");

        String body = null;
        if (from != null) {
            body = MessageFormat.format("Email: {1}\\n\\n{2}", new Object[] {from, feedback});
        } else {
            body = MessageFormat.format("{1}", new Object[] {feedback});
        }
        try {
            MailUtils.email(destination, "Feedback", body, from, props);
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            throw new ServiceException("Error sending feedback.");
        }
    }
}
