package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

/**
 * A sub-interface of Mine to encapsulate the mutable elements of the interface, and not
 * expose them the the consumer.
 * @author Alex Kalderimis
 *
 */
public interface ConfigurableMine extends Mine
{

    /**
     * Configure this mine with a the properties provided.
     * @param props The properties to consume.
     * @throws ConfigurationException If this mine has been configured wrong.
     */
    void configure(Properties props) throws ConfigurationException;
}
