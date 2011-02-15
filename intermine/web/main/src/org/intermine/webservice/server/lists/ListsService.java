package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.MemoryOutput;
import org.intermine.webservice.server.output.Output;


/**
 * Web service that returns all public lists (bags) that contain object with
 * specified id.
 * See {@link ListsRequestParser} for parameter description
 *  URL examples:
 *  <ul>
 *      <li>
 *  Get all public lists with specified intermine id: <br/>
 *  <code>/listswithobject?format=xml&amp;id=1343743</code>
 *      </li>
 *      <li>
 *  Get all public lists with specified id, corresponding intermine id is found with lookup: <br/>
 *  <code>/listswithobject?format=xml&amp;publicId=1343743</code>
 *      </li>
 *  </ul>
 * @author Jakub Kulaviak
 **/
public class ListsService extends WebService
{
    /**
     * Constructor
     * @param im The InterMine API
     */
    public ListsService(InterMineAPI im) {
        super(im);
    }

    /**
     * Executes service specific logic.
     * @param request request
     * @param response response
     */
    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response) {

        ListsServiceInput input = getInput();

        Integer objectId = null;
        if (input.getMineId() == null) {
            objectId = resolveMineId(request, input);
            if (objectId == null) {
                return;
            }
        } else {
            objectId = input.getMineId();
            if (!objectExists(request, objectId)) {
                throw new ResourceNotFoundException("object with specified id doesn't exist.");
            }
        }

        List<String> listNames = new ListManager(request).getListsNames(objectId);
        addListsToOutput(listNames);
        forward(input, output);
    }

    private boolean objectExists(HttpServletRequest request, Integer objectId) {
        ObjectStore os = this.im.getObjectStore();
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
        Model model = this.im.getModel();

        // checks  type
        if (model.getClassDescriptorByName(input.getType()) == null) {
            throw new BadRequestException("invalid " + ListsRequestParser.TYPE_PARAMETER
                    + " parameter." + " The specified type of the object doesn't exist: "
                    + input.getType());
        }

        PathQuery pathQuery = new PathQuery(model);
        pathQuery.addConstraint(Constraints.lookup(input.getType(), input.getPublicId(), null));
        pathQuery.addViews(getViewAccordingClasskeys(request, input.getType()));

        Profile profile = SessionMethods.getProfile(request.getSession());
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        Iterator<? extends List<ResultElement>> it = executor.execute(pathQuery);
        if (it.hasNext()) {
            List<ResultElement> row = it.next();
            if (it.hasNext()) {
                throw new BadRequestException("Multiple objects of type " + input.getType()
                        + " with public id " + input.getPublicId() + " were found.");
            }
            return row.get(0).getId();
        } else {
            throw new ResourceNotFoundException("No objects of type " + input.getType()
                    + " with public id " + input.getPublicId() + " were found.");
        }
    }

    private List<String> getViewAccordingClasskeys(HttpServletRequest request,
            String type) {
        List<String> ret = new ArrayList<String>();
        List<FieldDescriptor> descs = this.im.getClassKeys().get(type);
        for (FieldDescriptor desc : descs) {
            ret.add(type + "." + desc.getName());
        }
        return ret;
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
                throw new InternalErrorException(e);
            }
        }
    }

    private ListsServiceInput getInput() {
        return new ListsRequestParser(request).getInput();
    }
}
