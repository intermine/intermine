package org.intermine.objectstore.translating;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;

class DummyTranslator extends Translator
{
    public void setObjectStore(ObjectStore os) {
    }

    public Query translateQuery(Query query) throws ObjectStoreException {
        return query;
    }

    public Object translateToDbObject(Object o) {
        return o;
    }

    public Object translateFromDbObject(Object o) {
        return o;
    }

    public Object translateIdToIdentifier(Integer id) {
        return id;
    }
}
