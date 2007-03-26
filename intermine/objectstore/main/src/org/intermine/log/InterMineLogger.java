package org.intermine.log;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Query;

/**
 * Denotes the extensions required by this specialized intermine-event-logger.
 *
 * @author Peter McLaren
 * */
public interface InterMineLogger
{

    /**
     * Logs a simple message string.
     *
     * @param caller Ideally the name of the calling class or some helpful source identifier.
     * @param message Some hopefully useful text.
     * */
    public void logMessage(String caller, String message);

   /**
    * Logs the typical parameters that an object store query will be measured by.
    *
    * @param caller Ideally the name of the calling class or some helpful source identifier.
    * @param initiator Used to identify the user - could be a session id from the webapp.
    * @param oql Object Query Language version of the query.
    * @param sql SQL version of the query.
    *
    * @param optimise the number of milliseconds used to optimise the query.
    * @param estimated the estimated number of milliseconds required to run the query.
    * @param execute the number of milliseconds spent executing the query.
    * @param acceptable an acceptable number of milliseconds for the query to take.
    * @param conversion the number of milliseconds spent converting the results.
    * */
    public void logQuery(
           String caller, String initiator, Query oql, String sql,
           Long optimise, Long estimated, Long execute, Long acceptable, Long conversion);

}
