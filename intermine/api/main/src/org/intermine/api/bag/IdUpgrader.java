package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;

/**
 * Bag object id upgrader - find old objects in a new ObjectStore.
 *
 * @author Kim Rutherford
 */
public interface IdUpgrader
{
    /**
     * For the given object from an old ObjectStore, find the corresponding InterMineObjects in a
     * new ObjectStore.  Primary keys are used to find the objects.
     * @param oldObject the template object
     * @param os ObjectStore used to resolve objects
     * @return the set of new InterMineObjects
     */
    Set<Integer> getNewIds(InterMineObject oldObject, ObjectStore os);

    /**
     * Return true if upgrade should be performed
     * @return true if upgrade should be performed
     */
    boolean doUpgrade();

    /**
     * An upgrader that always fails.  For use when upgrading shouldn't be happening.
     */
    IdUpgrader ERROR_UPGRADER = new IdUpgrader() {
        @Override
        public Set<Integer> getNewIds(@SuppressWarnings("unused") InterMineObject oldObject,
                @SuppressWarnings("unused") ObjectStore objectStore) {
            throw new RuntimeException("Shouldn't call getNewIds() in a running webapp");
        }

        @Override
        public boolean doUpgrade() {
            return false;
        }
    };
}
