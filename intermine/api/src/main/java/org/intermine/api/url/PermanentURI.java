package org.intermine.api.url;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;

import java.util.Set;

/**
 * Class to represent the schema URI pattern adopted in InterMine.
 * Permanent URI pattern: domain/context/PermanentURI where
 * PermanentURI: className:external_local_id
 * Some examples:
 * humanmine.org/humanmine/protein:P31946-> PermanentURI=protein:P31946
 * humanmine.org/humanmine/geneontology:0000186-> PermanentURI=geneontology:0000186
 *
 * @author danielabutano
 */
public class PermanentURI
{
    private String className;
    private static final String LOCAL_ID_SEPARATOR = ":";
    private String identifier;
    private static final Logger LOGGER = Logger.getLogger(PermanentURI.class);

    /**
     * Constructor. Build a PermanentURI given prefix and LUI
     * @param className the className (e.g. Protein)
     * @param identifier the LUI (e.g. P31946, 0000186)
     */
    public PermanentURI(String className, String identifier) {
        this.className = className;
        this.identifier = identifier;
    }

    /**
     * Constructor. Build a PermanentURI after verifying the className
     * @param permanentURI URI as humanmine/protein:P31946 OR humanmine/Protein:P31946
     * @throws InvalidPermanentURLException if the permanentURI in input is not an permanent URI
    */
    public PermanentURI(String permanentURI) throws InvalidPermanentURLException {
        int localIdStartPosition = permanentURI.lastIndexOf("/") + 1;
        int localIdSeparatorPos = permanentURI.indexOf(LOCAL_ID_SEPARATOR, localIdStartPosition);
        if (localIdSeparatorPos == -1) {
            throw new InvalidPermanentURLException();
        }
        try {
            String classNameFromURI = permanentURI.substring(localIdStartPosition,
                    localIdSeparatorPos);
            className = getSimpleClassName(classNameFromURI);
            LOGGER.info("Given permanentURI : " + permanentURI
                    + " the simple class name is " + className);
            if (className == null) {
                throw new InvalidPermanentURLException();
            }
            identifier = permanentURI.substring(localIdSeparatorPos + 1);
        } catch (IndexOutOfBoundsException ex) {
            throw new InvalidPermanentURLException();
        }

    }

    private String getSimpleClassName(String className) {
        Model model = Model.getInstanceByName("genomic");
        Set<String> fullyQualifiedClassNames = model.getClassNames();
        for (String fullyQualifiedClassName : fullyQualifiedClassNames) {
            String simpleClassName = fullyQualifiedClassName.substring(
                    fullyQualifiedClassName.lastIndexOf(".") + 1);
            if (simpleClassName.toLowerCase().equals(className.toLowerCase())) {
                return simpleClassName;
            }
        }
        return null;
    }

    /**
     * Get the className
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the className
     * @param className the value to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the identifier
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the identifier (local unique identifier)
     * @param identifier the LUI
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the String which represents the PermanentURI -> className:identifier
     * @return the string in the format className:identifier
     */
    public String toString() {
        return className.toLowerCase() + LOCAL_ID_SEPARATOR + identifier;
    }
}
