package org.flymine.modelproduction;

import java.io.InputStream;

import org.flymine.metadata.Model;

/**
 * Common interface for parsing source models into FlyMine model format.
 *
 * @author Richard Smith
 */

public abstract class AbstractModelParser
{

    /**
     * Read source model information and construct a FlyMine Model object.
     * @param is the source model to parse
     * @return the FlyMine Model created
     * @throws Exception if Model not created successfully
     */
    public abstract Model process(InputStream is) throws Exception;

}
