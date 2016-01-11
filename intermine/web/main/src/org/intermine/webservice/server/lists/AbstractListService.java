package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.webservice.server.core.JSONService;

/**
 * Base class for list services.
 * @author Alex Kalderimis
 *
 */
public abstract class AbstractListService extends JSONService
{

    protected static final String LIST_NAME_KEY = "listName";
    protected static final String LIST_SIZE_KEY = "listSize";

    /** @param im The InterMine state object **/
    public AbstractListService(InterMineAPI im) {
        super(im);
    }

    /**
     * Get the classes represented by a collection of bags.
     * @param bags The bags.
     * @return The classes.
     */
    protected Set<ClassDescriptor> getClassesForBags(Collection<InterMineBag> bags) {
        Set<ClassDescriptor> classes = new HashSet<ClassDescriptor>();
        for (InterMineBag bag: bags) {
            ClassDescriptor cd = model.getClassDescriptorByName(bag.getType());
            classes.add(cd);
        }
        return classes;
    }

    /**
     * @return The Input for this request.
     */
    protected ListInput getInput() {
        return new ListInput(request, bagManager, getPermission().getProfile());
    }

}
