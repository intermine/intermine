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

import java.util.HashMap;

/**
 * Return all the existing bluegenes property or a specific one
 *
 * @author Daniela Butano
 */
public class BGPropertiesService extends JSONService
{
    /**
     * Constructor
     * @param im A reference to the InterMine API settings bundle
     */
    public BGPropertiesService(InterMineAPI im) {
        super(im);
    }
    @Override
    protected void execute() throws Exception {
        String key = getOptionalParameter("key");
        ObjectStoreWriter uosw = im.getProfileManager().getProfileObjectStoreWriter();
        String bgPropsAsString = MetadataManager.retrieve(
                ((ObjectStoreInterMineImpl) uosw).getDatabase(), MetadataManager.BG_PROPERTIES);
        if (bgPropsAsString == null) {
            addResultEntry(BGPropertiesServlet.BG_PROPS, new HashMap<>(), false);
        }
        else {
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, String> bgMap = mapper.readValue(bgPropsAsString, HashMap.class);
            if (key == null) {
                addResultEntry(BGPropertiesServlet.BG_PROPS, bgMap, false);
            } else {
                String value = bgMap.get(key);
                if (value == null) {
                    throw new ResourceNotFoundException("A property with key " + key
                            + " doesn't exists.");
                }
                addResultEntry(BGPropertiesServlet.BG_PROPS, (new HashMap<>()).put(key, value),
                        false);
            }

        }
    }
}
