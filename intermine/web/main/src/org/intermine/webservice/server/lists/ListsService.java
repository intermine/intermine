package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
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
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;


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
public class ListsService extends AvailableListsService
{
    /**
     * Constructor
     * @param im The InterMine API
     */
    public ListsService(final InterMineAPI im) {
        super(im);
    }

    /**
     * Executes service specific logic.
     * @return The lists relevant to this request.
     */
    @Override
    protected Collection<InterMineBag> getLists() {

        final ListsServiceInput input = getInput();

        Integer objectId = null;
        if (input.getMineId() == null) {
            objectId = resolveMineId(request, input);
            if (objectId == null) {
                throw new ResourceNotFoundException("object with specified id doesn't exist.");
            }
        } else {
            objectId = input.getMineId();
            if (!objectExists(request, objectId)) {
                throw new ResourceNotFoundException("object with specified id doesn't exist.");
            }
        }

        return new ListManager(im, getPermission().getProfile())
                .getListsContaining(objectId);
    }

    private boolean objectExists(final HttpServletRequest request, final Integer objectId) {
        final ObjectStore os = im.getObjectStore();
        try {
            final InterMineObject objectById = os.getObjectById(objectId);
            return objectById != null;
        } catch (final ObjectStoreException e) {
            throw new RuntimeException("Getting object with id " + objectId + " failed.");
        }
    }

    private Integer resolveMineId(final HttpServletRequest request,
            final ListsServiceInput input) {
        final Model model = im.getModel();

        // checks  type
        if (model.getClassDescriptorByName(input.getType()) == null) {
            throw new BadRequestException("invalid " + ListsRequestParser.TYPE_PARAMETER
                    + " parameter." + " The specified type of the object doesn't exist: "
                    + input.getType());
        }

        final PathQuery pathQuery = new PathQuery(model);
        pathQuery.addConstraint(Constraints.lookup(input.getType(),
                input.getPublicId(), input.getExtraValue()));
        pathQuery.addViews(getViewAccordingClasskeys(request, input.getType()));

        final Profile profile = getPermission().getProfile();
        final PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        final Iterator<? extends List<ResultElement>> it = executor.execute(pathQuery);
        if (it.hasNext()) {
            final List<ResultElement> row = it.next();
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

    private List<String> getViewAccordingClasskeys(final HttpServletRequest request,
            final String type) {
        final List<String> ret = new ArrayList<String>();
        final List<FieldDescriptor> descs = im.getClassKeys().get(type);
        for (final FieldDescriptor desc : descs) {
            ret.add(type + "." + desc.getName());
        }
        return ret;
    }

    private ListsServiceInput getInput() {
        return new ListsRequestParser(request).getInput();
    }
}
