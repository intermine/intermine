package org.intermine.web.uri;

/*
 * Copyright (C) 2002-2021 FlyMine
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
import org.intermine.util.DynamicUtil;
import org.intermine.web.context.InterMineContext;

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
     * @param profile the profile
     */
    public InterMineLUIConverter(Profile profile) {
        this.profile = profile;
        classNameIdentifierMapper = ClassNameURIIdentifierMapper.getMapper();
    }

    /**
     * Constructor
     */
    public InterMineLUIConverter() {
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
        String identifier = getIdentifier(className);
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
     */
    public InterMineLUI getInterMineLUI(Integer interMineID) {
        if (interMineID == null) {
            LOGGER.error("intermineID is null");
            return null;
        }
        String type = null;
        String identifier = null;
        try {
            InterMineAPI im = getInterMineAPI();
            InterMineObject entity = im.getObjectStore().getObjectById(interMineID);
            if (entity == null) {
                return null;
            }
            type = DynamicUtil.getSimpleClass(entity).getSimpleName();
            String identifierField = getIdentifier(type);
            identifier = (String) entity.getFieldValue(identifierField);
            if (identifier == null) {
                LOGGER.info("The entity " + interMineID + " has " + identifierField + " null, "
                        + "the share link will not displayed in the report page. Configure a "
                        + "different key in the class_keys.properties file");
                return null;
            }
        } catch (ObjectStoreException ose) {
            LOGGER.error("Failed to find object with id: " + interMineID, ose);
            return null;
        }  catch (IllegalAccessException iae) {
            LOGGER.error("Failed to get identifier fot the entity with id: " + interMineID, iae);
            return null;
        }

        return new InterMineLUI(type, identifier);
    }

    /**
     * Return the identifier's field of the entity with type given in input
     * @param type the type of the entity,e.g. Protein
     * @return the identifier's field if specified or primaryIdentifier
     */
    private String getIdentifier(String type) {
        String identifier = classNameIdentifierMapper.getIdentifier(type);
        return ( identifier != null) ? identifier : DEFAULT_IDENTIFIER;
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
