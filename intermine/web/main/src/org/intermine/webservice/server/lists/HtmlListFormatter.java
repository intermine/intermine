package org.intermine.webservice.server.lists;

import java.util.Arrays;
import java.util.List;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStoreException;

public class HtmlListFormatter implements ListFormatter {

    @Override
    public List<String> format(InterMineBag list) {
        String size;
        try {
            size = String.valueOf(list.getSize());
        } catch (ObjectStoreException e) {
            size = "Unknown";
        }
        return Arrays.asList(
            list.getName(), list.getType(),
            list.getDescription(), size
        );
    }

    @Override
    public void setSize(int size) {
        // No-op implementation
    }

}
