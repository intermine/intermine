package org.intermine.webservice.server.lists;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.webservice.server.core.JSONService;

public abstract class AbstractListService extends JSONService {

    public static final String LIST_SEPARATOR = ";";
    public static final String TEMP_SUFFIX = "_temp";
    protected static final String LIST_NAME_KEY = "listName";
    protected static final String LIST_SIZE_KEY = "listSize";


    public AbstractListService(InterMineAPI im) {
        super(im);
    }

    protected Set<ClassDescriptor> getClassesForBags(Collection<InterMineBag> bags) {
        Set<ClassDescriptor> classes = new HashSet<ClassDescriptor>();
        for (InterMineBag bag: bags) {
            ClassDescriptor cd = model.getClassDescriptorByName(bag.getType());
            classes.add(cd);
        }
        return classes;
    }

    protected ListInput getInput(HttpServletRequest request) {
        return new ListInput(request, bagManager, getPermission().getProfile());
    }

}
