package org.intermine.webservice.lists;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.webservice.WebService;
import org.intermine.webservice.WebServiceConstants;
import org.intermine.webservice.WebServiceException;
import org.intermine.webservice.core.ListManager;
import org.intermine.webservice.core.PathQueryExecutor;
import org.intermine.webservice.output.MemoryOutput;
import org.intermine.webservice.output.Output;


/**
 * Web service that returns all public lists (bags) that contain object with
 * specified id.
 * See {@link ListsRequestProcessor} for parameter description
 *  URL examples: 
 *  Get all public lists with specified intermine id
 *  /listswithobject?output=xml&id=1343743 
 *  Get all public lists with specified id, corresponding intermine id is found with lookup
 *  /listswithobject?output=xml&publicId=1343743
 * @author Jakub Kulaviak
 **/
public class ListsService extends WebService
{
    
    /**
     * Executes service specific logic. 
     * @param request request
     * @param response response
     */
    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response) {
        
        ListsServiceInput input = getInput();
        if (!validate(input)) {
            return;
        }
        
        Integer objectId = null;
        if (input.getMineId() == null) {
            objectId = resolveMineId(request, input);
            if (objectId == null) {
                return;
            }
        } else {
            objectId = input.getMineId();
            if (!objectExists(request, objectId)) {
                output.addError("object with specified id doesn't exist.", Output.SC_NOT_FOUND);
                output.flush();
                return;
            }
        }
        
        List<String> listNames = new ListManager(request).getListsNames(objectId);
        addListsToOutput(listNames);
        forward(input, output);
    }

    private boolean objectExists(HttpServletRequest request, Integer objectId) {
        ObjectStore os = (ObjectStore) request.getSession().
            getServletContext().getAttribute(Constants.OBJECTSTORE);
        try {
            InterMineObject objectById = os.getObjectById(objectId);
            return objectById != null;
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Getting object with id " + objectId + " failed.");
        }
    }

    private void addListsToOutput(List<String> listNames) {
        for (String name : listNames) {
            List<String> result = new ArrayList<String>();
            result.add(name);
            output.addResultItem(result);
        }
    }

    private Integer resolveMineId(HttpServletRequest request,
            ListsServiceInput input) {
        ObjectStore os = (ObjectStore) request.getSession().getServletContext().
            getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();

        // checks  type
        if (model.getClassDescriptorByName(input.getType()) == null) {
            output.addError("invalid " + ListsRequestParser.TYPE_PARAMETER + " parameter." 
                    + " Specified type of the object doesn't exist: " + input.getType(), 
                    Output.SC_BAD_REQUEST);
            output.flush();
            return null;                
        }
        
        PathQuery pathQuery = new PathQuery(model);
        PathNode node = new PathNode(input.getType());
        Constraint constraint = new Constraint(ConstraintOp.LOOKUP, input.getPublicId());
        node.getConstraints().add(constraint);
        pathQuery.getNodes().put(input.getType(), node);
        pathQuery.addPathStringToView(input.getType());
        PathQueryExecutor executor = new PathQueryExecutor(request, pathQuery);
        Results results = executor.getResults();
        if (results.size() != 1) {
            if (results.size() == 0) {
                output.addError("No objects of type " + input.getType() + " with public id " 
                        + input.getPublicId() + " were found.", Output.SC_NOT_FOUND);
            } else {
                output.addError("Multiple objects of type " + input.getType() + " with public id " 
                        + input.getPublicId() + " were found.", Output.SC_BAD_REQUEST);
            }
            output.flush();
            return null;
        } else {
            ResultsRow row = (ResultsRow) results.get(0);
            return ((InterMineObject) row.get(0)).getId();
        }
    }

    private void forward(ListsServiceInput input, Output output) {
        if (getFormat() == HTML_FORMAT) {
            MemoryOutput mout = (MemoryOutput) output;
            request.setAttribute("rows", mout.getResults());
            List<String> columnNames = new ArrayList<String>();
            columnNames.add("List");
            request.setAttribute("columnNames", columnNames);
            request.setAttribute("title", "Lists with " + input.getPublicId());
            try {
                getHtmlForward().forward(request, response);
            } catch (Exception e) {
                throw new WebServiceException(WebServiceConstants.SERVICE_FAILED_MSG, e);
            } 
        }
    }

    private ListsServiceInput getInput() {
        return new ListsRequestParser(request).getInput();
    }
}
