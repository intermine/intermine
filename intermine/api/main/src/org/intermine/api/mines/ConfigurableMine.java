package org.intermine.api.mines;

import java.util.Properties;

/**
 * A sub-interface of Mine to encapsulate the mutable elements of the interface, and not
 * expose them the the consumer.
 * @author Alex Kalderimis
 *
 */
public interface ConfigurableMine extends Mine {

    /**
     * Configure this mine with a the properties provided.
     * @param props The properties to consume.
     * @throws ConfigurationException If this mine has been configured wrong.
     */
    public void configure(Properties props) throws ConfigurationException;
}
