package org.intermine.bio.chado.config;

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
 * An action that creates a collection of objects from value.
 * @author Kim Rutherford
 */
public class CreateCollectionAction extends MatchingFieldConfigAction
{
    private final String fieldName;
    private final String className;
    private final String referenceName;
    private final boolean createSingletons;

    /**
     * Create a new CreateCollectionAction object.
     * @param className the class name of the object to create for each new value
     * @param referenceName the name of the reference or collection to set or to add the new
     * object to
     * @param fieldName the field name to set in the new object
     * @param createSingletons if true, create only one object of class className with each
     * possible fieldName; if false, multiple objects with the same value for fieldName might
     * be created
     */
    public CreateCollectionAction(String className, String referenceName, String fieldName,
                                  boolean createSingletons) {
        super(null);
        this.className = className;
        this.referenceName = referenceName;
        this.fieldName = fieldName;
        this.createSingletons = createSingletons;
    }

    /**
     * Return the fieldName that was passed to the constructor.
     * @return the fieldName
     */
    public final String getFieldName() {
        return fieldName;
    }

    /**
     * Return the className that was passed to the constructor.
     * @return the className
     */
    public final String getClassName() {
        return className;
    }

    /**
     * Return the referenceName that was passed to the constructor.
     * @return the referenceName
     */
    public final String getReferenceName() {
        return referenceName;
    }

    /**
     * If true, create only one object of class className with each possible fieldName;
     * if false, multiple objects with the same value for fieldName might be created
     * @return true iff the new items should be singletons
     */
    public final boolean createSingletons() {
        return createSingletons;
    }
}
