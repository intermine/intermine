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

/**
 * A description of an OAuth Provider that we can use as a source
 * of identity information.
 *
 * @author Alex Kalderimis
 *
 */
public interface OAuthProvider
{

    /** @return the name of the provider **/
    String getName();

    /** @return the URL for the token endpoint **/
    String getTokenUrl();

    /** @return the configured client id for this provider **/
    String getClientId();

    /** @return the configured client secret for this provider **/
    String getClientSecret();

    /** @return the message format this provider expects. **/
    MessageFormat getMessageFormat();

    /** @return the format in which this provider responds. **/
    ResponseType getResponseType();
}
