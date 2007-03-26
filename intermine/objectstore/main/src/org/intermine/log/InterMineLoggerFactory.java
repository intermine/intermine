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

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.PropertiesUtil;
import org.apache.log4j.Logger;

import java.util.Properties;
import java.lang.reflect.Method;

/**
 * Standard InterMine style factory that provides InterMineLogger instances.
 *
 * @author Peter McLaren
 */
public class InterMineLoggerFactory
{
    private static final Logger LOG = Logger.getLogger(InterMineLoggerFactory.class);

    /**
     * Gets the default logger
     *
     * @throws Exception if there is a problem
     * @return An intermine logger
     * */
    public static InterMineLogger getInterMineLogger() throws Exception {

        return getInterMineLogger("logger.default");
    }

    /**
     * Gets the logger matching the supplied alias
     *
     * @param loggerAlias the alias of the logger required
     * @throws Exception if there is a problem
     * @return An intermine logger
     * */
    public static InterMineLogger getInterMineLogger(String loggerAlias) throws Exception {

        if (loggerAlias == null || "".equals(loggerAlias)) {
            throw new Exception("InterMineLoggerFactory supplied an invalid alias! " + loggerAlias);
        }

        LOG.debug("InterMineLoggerFactory.getInterMineLogger() called, loggerAlias:" + loggerAlias);

        Properties props = PropertiesUtil.getPropertiesStartingWith(loggerAlias);

        if (0 == props.size()) {
            throw new ObjectStoreException(
                    "No ObjectStore properties were found for alias '" + loggerAlias + "'");
        }

        props = PropertiesUtil.stripStart(loggerAlias, props);
        String clsName = props.getProperty("class");
        if (clsName == null) {
            throw new ObjectStoreException(loggerAlias
                    + " does not have an InterMineLogger class specified (check properties file)");
        }
        Class cls = null;

        String oswAlias = props.getProperty("osw");
        if (oswAlias == null) {
            throw new ObjectStoreException(loggerAlias
                    + " does not have an InterMineLogger osw specified (check properties file)");
        }

        try {
            cls = Class.forName(clsName);
        } catch (ClassNotFoundException e) {
            throw new ObjectStoreException("Cannot find specified ObjectStore class '" + clsName
                    + "' for " + loggerAlias + " (check properties file)", e);
        }

        Class[] parameterTypes = new Class[] {String.class};
        Method m = cls.getDeclaredMethod("getInstance", parameterTypes);

        return (InterMineLogger) m.invoke(null, new Object[] {oswAlias});
    }

}
