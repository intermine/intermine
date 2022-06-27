package org.intermine.web.uri;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Set;

/**
 * Class to represent the InterMine Local Unique Identifier
 * InterMineLUI schema: className:identifier  where the identifier is the id
 * provided by the data source provider, in some cases, adapted with a prefix e.g.RGD:62030
 * Some examples:
 * humanmine.org/humanmine/protein:P31946-> InterMineLUI=protein:P31946
 * humanmine.org/humanmine/gene:RGD:62030-> InterMineLUI=gene:RGD:62030
 *
 * @author danielabutano
 */
public class InterMineLUI
{
    private String className;
    private static final String LOCAL_ID_SEPARATOR = ":";
    private String identifier;

    /**
     * Constructor. Build a InterMineLUI given prefix and LUI
     * @param className the className (e.g. Protein)
     * @param identifier the identifier (e.g. P31946)
     */
    public InterMineLUI(String className, String identifier) {
        this.className = className;
        this.identifier = identifier;
    }

    /**
     * Constructor. Build a InterMineLUI after verifying the className
     * @param permanentURI URI as humanmine/protein:P31946 OR humanmine/Protein:P31946
     * @throws InvalidPermanentURLException if the permanentURI in input is not an permanent URI
    */
    public InterMineLUI(String permanentURI) throws InvalidPermanentURLException {
        int localIdStartPosition = permanentURI.lastIndexOf("/") + 1;
        int localIdSeparatorPos = permanentURI.indexOf(LOCAL_ID_SEPARATOR, localIdStartPosition);
        if (localIdSeparatorPos == -1) {
            throw new InvalidPermanentURLException();
        }
        try {
            String classNameFromURI = permanentURI.substring(localIdStartPosition,
                    localIdSeparatorPos);
            className = getSimpleClassName(classNameFromURI);
            if (className == null) {
                throw new InvalidPermanentURLException();
            }
            String encodedIdentifier = permanentURI.substring(localIdSeparatorPos + 1);
            try {
                identifier = URLDecoder.decode(encodedIdentifier, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                identifier = encodedIdentifier;
            }
        } catch (IndexOutOfBoundsException ex) {
            throw new InvalidPermanentURLException();
        }

    }

    /**
     * Given a type, which might not contain capital letters, return the class name as it is
     * defined in the model
     * @param className which might not contain capital letters (e.g. dataset)
     * @return the class name as it is defined in the model (e.g. DataSet)
     */
    protected static String getSimpleClassName(String className) {
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
     * @return the identifier provided by the data source provider and eventually adapted
     * by InterMine
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the identifier
     * @param identifier the identifier provided by the data source provider and eventually adapted
     * by InterMine
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the String which represents the InterMineLUI -> className:identifier
     * @return the string in the format className:identifier
     */
    public String toString() {
        try {
            String encodedIdentifier = URLEncoder.encode(identifier, "UTF-8");
            // The URLEncoder class is based on RFC 2396, and there are few differences
            // between the unreserved characters in case of RFC 2396 and RFC 3986
            encodedIdentifier = encodedIdentifier.replaceAll("%3A", ":")
                    .replaceAll("\\+", "%20");
            return className.toLowerCase() + LOCAL_ID_SEPARATOR + encodedIdentifier;
        } catch (UnsupportedEncodingException ex) {
            return className.toLowerCase() + LOCAL_ID_SEPARATOR + identifier;
        }
    }
}
