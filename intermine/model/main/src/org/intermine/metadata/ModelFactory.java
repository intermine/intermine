package org.intermine.metadata;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A class that loads a model given a name.
 * @author Alex Kalderimis
 *
 */
public final class ModelFactory
{

    private ModelFactory() {
        // Hidden.
    }
    /**
     * Name of the key under which to store the serialized version of the model
     */
    private static final String MODEL = "model";

    /**
     * Load a named model from the classpath
     * @param name the model name
     * @return the model
     */
    public static Model loadModel(String name) {
        String filename = Util.getFilename(MODEL, name);
        InputStream is = Model.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            throw new IllegalArgumentException("Model definition file '" + filename
                                               + "' cannot be found");
        }
        Model model = null;
        try {
            model = new InterMineModelParser().process(new InputStreamReader(is));
        } catch (Exception e) {
            throw new RuntimeException("Error parsing model definition file '" + filename + "'", e);
        }
        return model;
    }

}
