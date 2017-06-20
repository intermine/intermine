package org.intermine.web.struts.oauth2;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.apache.struts.action.ActionMessage;

/**
 * An exception type to plaster over the absence of first-class
 * continuations in java.
 *
 * Represents a problem that has been foreseen, and handled.
 *
 * @author Alex Kalderimis
 */
public class ForseenProblem extends Exception
{

    /**
     * serialisable's magic number.
     */
    private static final long serialVersionUID = -8773338970214227633L;

    private String msgKey;

    private Object[] args;

    /**
     * Construct one of these objects.
     * @param msgKey The action message key to use.
     */
    public ForseenProblem(String msgKey) {
        this.msgKey = msgKey;
        this.args = new Object[0];
    }

    /**
     * Construct one of these objects.
     * @param msgKey The action message key.
     * @param args The arguments to the action message.
     */
    public ForseenProblem(String msgKey, Object... args) {
        this.msgKey = msgKey;
        this.args = args;
    }

    /**
     * Get a struts action message, rather than a simple string message.
     * @return an action message for saying something to the user.
     */
    public ActionMessage getActionMessage() {
        return new ActionMessage(msgKey, args);
    }
}
