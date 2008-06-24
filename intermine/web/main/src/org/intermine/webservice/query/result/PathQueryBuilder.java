package org.intermine.webservice.query.result;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.ServletMethods;
import org.intermine.web.logic.bag.InterMineBag;


/**
 * PathQueryBuilder builds PathQuery object from xml and validates it. 
 * @author Jakub Kulaviak
 **/
public class PathQueryBuilder 
{

    private List<String> errors = new ArrayList<String>();
    private PathQuery pathQuery;

    /**
     * PathQueryBuilder constructor. 
     * @param xml xml string from which will be PathQuery constructed
     * @param schemaUrl url of XML Schema file, validation is performed according this file 
     * @param servletContext object from which other objects and parameters are obtained
     * @param savedBags previously saved bags  
     */
    public PathQueryBuilder(String xml, String schemaUrl, ServletContext servletContext, 
            Map<Object, InterMineBag> savedBags) {
        buildQuery(xml, schemaUrl, servletContext, savedBags);
    }

    private void buildQuery(String xml, String schemaUrl, ServletContext servletContext, 
            Map<Object, InterMineBag> savedBags) {
        XMLValidator validator = new XMLValidator();
        validator.validate(xml, schemaUrl);
        if (validator.getErrorsAndWarnings().size() == 0) {
            try {
                pathQuery = ServletMethods.fromXml(xml, savedBags,
                        servletContext);                
            } catch (Exception ex) {
                errors.add("XML is well formatted but contains invalid model data. "
                        + "Check that your constraints are correct "
                        + "and query corresponds to model. Cause:" + ex.getMessage());
                return;
            }
            if (!pathQuery.isValid()) {
                errors = convertProblems(pathQuery.getProblems());
            }
        } else {
            errors = validator.getErrorsAndWarnings();
        }
    }

    /**
     * Returns true if query is valid.
     * @return true if query  is valid
     */
    public boolean isQueryValid() {
        return getErrors().size() == 0;
    }

    /**
     * Returns parsed PathQuery. 
     * @return parsed PathQuery
     */
    public PathQuery getQuery() {
        return pathQuery;
    }

    /**
     * Returns errors that occurred during xml parsing.
     * @return errors
     */
    public List<String> getErrors() {
        return errors;
    }

    private List<String> convertProblems(Throwable[] problems) {
        List<String> ret = new ArrayList<String>();
        for (int i = 0; i < problems.length; i++) {
            ret.add(problems[i].getMessage());
        }
        return ret;
    }
}
