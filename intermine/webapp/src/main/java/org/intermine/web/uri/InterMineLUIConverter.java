package org.intermine.web.uri;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.context.InterMineContext;

import java.lang.reflect.Field;

/**
 * This class converts an intermineID into a InterMineLUI
 * or a InterMineLUI into an intermineID.
 *
 * @author Daniela Butano
 */
public class InterMineLUIConverter
{
    private static final Integer INTERMINE_ID_NOT_FOUND = -1;
    private static final String DEFAULT_IDENTIFIER = "primaryIdentifier";
    private ClassNameURIIdentifierMapper classNameIdentifierMapper = null;
    private static final Logger LOGGER = Logger.getLogger(InterMineLUIConverter.class);
    protected Profile profile = null;

    /**
     * Constructor
     * @param profile th eprofile
     */
    public InterMineLUIConverter(Profile profile) {
        this.profile = profile;
        classNameIdentifierMapper = ClassNameURIIdentifierMapper.getMapper();
    }

    /**
     * Given a InterMineLUI (compact uri, e.g. protein:P27362) returns the internal intermine Id
     * @param interMineLUI the interMineLUI
     * @return internal intermine id OR -1 if there is no entity matching with the LUI or if there
     * is no identifier set in the class_keys.properties file for the class defined in the lui
     * @throws ObjectStoreException if there are any objectstore issues
     */
    public Integer getInterMineID(InterMineLUI interMineLUI) throws ObjectStoreException {
        if (interMineLUI == null) {
            throw new RuntimeException("InterMineLUI is null");
        }
        PathQuery pathQuery = new PathQuery(getModel());
        String className = interMineLUI.getClassName();
        String viewPath =  className + ".id";
        pathQuery.addView(viewPath);
        String identifier = classNameIdentifierMapper.getIdentifier(className);
        if (identifier == null) {
            identifier = DEFAULT_IDENTIFIER;
        }
        String constraintPath = className + "." + identifier;
        pathQuery.addConstraint(Constraints.eq(constraintPath, interMineLUI.getIdentifier()));
        if (!pathQuery.isValid()) {
            LOGGER.info("The PathQuery :" + pathQuery.toString() + " is not valid. No "
                    + className + "_URI defined in the class_key.properties file");
            return INTERMINE_ID_NOT_FOUND;
        }
        LOGGER.info("InterMineLUIConverter: pathQuery to retrieve internal id: "
                + pathQuery.toString());
        ExportResultsIterator iterator = getPathQueryExecutor().execute(pathQuery);
        if (iterator.hasNext()) {
            ResultElement row = iterator.next().get(0);
            return row.getId();
        } else {
            LOGGER.info("InterMineLUIConverter: there are no " + className
                    + " with " + constraintPath + "=" + interMineLUI.getIdentifier());
            return INTERMINE_ID_NOT_FOUND;
        }
    }

    /**
     * Generate the InterMineLUI associated to the internal interMine ID
     * @param interMineID the interMineID
     * @return the InterMineLUI
     * @throws ObjectStoreException if something goes wrong when retrieving the identifier
     */
    public InterMineLUI getInterMineLUI(Integer interMineID) throws ObjectStoreException {
        if (interMineID == null) {
            LOGGER.error("intermineID is null");
            return null;
        }
        String type = getType(interMineID);
        if (type == null) {
            LOGGER.error("The entity " + interMineID + " does not exits");
            return null;
        }
        String identifier = getIdentifier(type, interMineID);
        if (identifier == null) {
            LOGGER.info("The identifier's value for the entity " + interMineID + " is null");
            return null;
        }
        return new InterMineLUI(type, identifier);
    }

    /**
     * Return the type of the entity with the id given in input
     * @param interMineID the interMineID
     * @return the type, e.g. Protein
     */
    private String getType(Integer interMineID) {
        InterMineObject imObj = null;
        String type = null;
        try {
            InterMineAPI im = getInterMineAPI();
            InterMineObject obj = im.getObjectStore().getObjectById(interMineID);
            if (obj == null) {
                return null;
            }
            Class shadowClass = obj.getClass();
            try {
                Field field = shadowClass.getField("shadowOf");
                Class clazz = (Class) field.get(null);
                type = clazz.getSimpleName();
                LOGGER.info("InterMineLUIConverter: the entity with id " + interMineID
                        + " has type: " + type);
            } catch (NoSuchFieldException e) {
                LOGGER.info(e);
                type = shadowClass.getSimpleName();
            }  catch (IllegalAccessException e) {
                LOGGER.error(e);
            }
        } catch (ObjectStoreException e) {
            LOGGER.error("Failed to find the object with id: " + interMineID, e);
        }
        return type;
    }

    /**
     * Generate the InterMineLUI associated given the type and the internal interMine ID
     * @param type the class
     * @param interMineID the interMineID
     * @return the InterMineLUI
     * @throws ObjectStoreException if something goes wrong when retrieving the identifier
     */
    public InterMineLUI getInterMineLUI(String type, Integer interMineID)
            throws ObjectStoreException {
        if (interMineID == null) {
            LOGGER.error("intermineID is null");
            return null;
        }
        if (type == null) {
            LOGGER.error("The type is null");
            return null;
        }
        //check first if type is in the model
        type = InterMineLUI.getSimpleClassName(type);
        if (type == null) {
            LOGGER.error("The type is not defined in the model");
            return null;
        }
        String identifier = getIdentifier(type, interMineID);
        if (identifier == null) {
            return null;
        }
        return new InterMineLUI(type, identifier);
    }


    /**
     * Return the identifier's value of the entity specified by the interMineId given in input
     * @param type the type of the entity,e.g. Protein
     * @param interMineId the interMineId which identifies the entity
     * @return the identifier's value
     * @throws ObjectStoreException if something goes wrong with query retrieving identifier
     */
    private String getIdentifier(String type, Integer interMineId) throws ObjectStoreException {
        String identifier = classNameIdentifierMapper.getIdentifier(type);
        if (identifier == null) {
            identifier = DEFAULT_IDENTIFIER;
        }
        PathQuery pathQuery = new PathQuery(getModel());
        String viewPath = type + "." + identifier;
        pathQuery.addView(viewPath);
        String constraintPath = type + ".id";
        pathQuery.addConstraint(Constraints.eq(constraintPath, Integer.toString(interMineId)));
        if (!pathQuery.isValid()) {
            LOGGER.info("The PathQuery :" + pathQuery.toString() + " is not valid");
            LOGGER.info("For the entity with type " + type
                    + " is not possible to generate a permanent URI.");
            return null;
        }

        ExportResultsIterator iterator = getPathQueryExecutor().execute(pathQuery);
        if (iterator.hasNext()) {
            ResultElement cell = iterator.next().get(0);
            return (String) cell.getField();
        }
        LOGGER.info("No entity with type " + type + " and id " + interMineId);
        return null;
    }

    /**
     * Returns the model
     * @return the model
     */
    protected Model getModel() {
        return Model.getInstanceByName("genomic");
    }

    /**
     * Returns the InterMineAPI
     * @return the InterMineAPI
     */
    protected InterMineAPI getInterMineAPI() {
        return InterMineContext.getInterMineAPI();
    }

    /**
     * Returns the PathQueryExecutor
     * @return the PathQueryExecutor
     */
    public PathQueryExecutor getPathQueryExecutor() {
        InterMineAPI im = getInterMineAPI();
        return new PathQueryExecutor(im.getObjectStore(), profile, null, im.getBagManager());
    }
}
