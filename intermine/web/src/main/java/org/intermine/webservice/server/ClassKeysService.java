package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.json.JSONObject;

/**
 * A service to fetch a JSON representation of class keys for all classes in the model.
 *
 * @author Alexis Kalderimis
 */
public class ClassKeysService extends SummaryService
{

    /**
     * Construct with the InterMineAPI.
     * @param im the InterMineAPI
     */
    public ClassKeysService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        if (classKeys == null) {
            throw new ServiceException("class keys unavailable");
        }
        Map<String, List<String>> ckData = new HashMap<String, List<String>>();
        Model m = im.getModel();
        output.setHeaderAttributes(getHeaderAttributes());
        for (ClassDescriptor cd : m.getClassDescriptors()) {
            List<String> keyFields = new ArrayList<String>();
            String cname = cd.getUnqualifiedName();
            if (!"InterMineObject".equals(cname)) {
                if (classKeys.containsKey(cname)) {
                    for (FieldDescriptor fd : classKeys.get(cname)) {
                        keyFields.add(cname + "." + fd.getName());
                    }
                    ckData.put(cname, keyFields);
                }
            }
        }

        JSONObject jo = new JSONObject(ckData);
        output.addResultItem(Collections.singletonList(jo.toString()));
    }

}
