package org.intermine.webservice.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.json.JSONObject;

public class ClassKeysService extends SummaryService {

    public ClassKeysService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        if (classKeys == null) {
            throw new InternalErrorException("class keys unavailable");
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
