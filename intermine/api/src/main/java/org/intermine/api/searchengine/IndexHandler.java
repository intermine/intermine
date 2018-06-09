package org.intermine.api.searchengine;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for handling indexes.
 *
 * @author arunans23
 */

public interface IndexHandler
{
    /**
     *
     * @param os Objectstore that is passed CreateSearchIndexTask
     */
    public void createIndex(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys) throws IOException;

}
