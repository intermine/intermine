package org.intermine.web.bag;

import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;

public interface IdUpgrader {
	
	  /**
     * For the given object from an old ObjectStore, find the corresponding InterMineObjects in a
     * new ObjectStore.  Primary keys are used to find the objects.
     * @param oldObject the template object
     * @param os ObjectStore used to resolve objects
     * @return the set of new InterMineObjects
     */
    public Set getNewIds(InterMineObject oldObject, ObjectStore os);
}
