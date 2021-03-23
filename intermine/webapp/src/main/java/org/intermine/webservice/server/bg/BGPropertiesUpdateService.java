package org.intermine.webservice.server.bg;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intermine.api.InterMineAPI;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

import java.util.HashMap;

/**
 * Update an existing bluegenes property.
 *
 * @author Daniela Butano
 */
public class BGPropertiesUpdateService extends JSONService
{
    /**
     * Constructor
     * @param im A reference to the InterMine API settings bundle
     */
    public BGPropertiesUpdateService(InterMineAPI im) {
        super(im);
    }
    @Override
    protected void execute() throws Exception {
        if (!getPermission().getProfile().isSuperuser()) {
            throw new ServiceForbiddenException("Only admins users can access this service");
        }

        String key = getRequiredParameter("key");
        String value = getRequiredParameter("value");

        ObjectStoreWriter uosw = im.getProfileManager().getProfileObjectStoreWriter();
        String bgPropsAsString = MetadataManager.retrieve(
                ((ObjectStoreInterMineImpl) uosw).getDatabase(), MetadataManager.BG_PROPERTIES);
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, String> bgMap;
        if (bgPropsAsString == null) {
            throw new ResourceNotFoundException("A property with key " + key + " doesn't exists.");
        } else {
            bgMap = mapper.readValue(bgPropsAsString, HashMap.class);
            if (!bgMap.containsKey(key)) {
                throw new ResourceNotFoundException("A property with key " + key
                        + " doesn't exists.");
            }
            bgMap.put(key, value);
            MetadataManager.store(((ObjectStoreInterMineImpl) uosw).getDatabase(),
                        MetadataManager.BG_PROPERTIES, mapper.writeValueAsString(bgMap));
        }
    }
}
