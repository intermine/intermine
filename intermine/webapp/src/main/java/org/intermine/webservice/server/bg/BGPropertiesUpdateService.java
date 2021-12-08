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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.json.JSONObject;

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
        String body = IOUtils.toString(request.getReader());
        String key = null;
        String value = null;
        if (!StringUtils.isEmpty(body)) {
            String[] input = parseInput(body);
            key = input[0];
            value = input[1];
        }
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            key = getRequiredParameter("key");
            value = getRequiredParameter("value");
        }

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

    private String[] parseInput(String input) {
        int keyIndex = input.indexOf("key");
        int valueIndex = input.indexOf("value");
        int separatorIndex = input.indexOf("&");
        String key, value;
        if (keyIndex < valueIndex) {
            key = input.substring(keyIndex + 4, separatorIndex);
            value = input.substring(valueIndex + 6);
        } else {
            value = input.substring(valueIndex + 6, separatorIndex);
            key = input.substring(keyIndex + 4);
        }
        return new String[] {key, value};
    }
}
