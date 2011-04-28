package org.intermine.webservice.server.lists;

import java.util.Arrays;
import java.util.List;

import org.intermine.api.profile.InterMineBag;

/**
 * Formats a list into its name.
 * @author Alex Kalderimis
 *
 */
public class FlatListFormatter implements ListFormatter {

    @Override
    public List<String> format(InterMineBag list) {
        return Arrays.asList(list.getName());
    }

    @Override
    public void setSize(int size) {
        // No-op implementation.
    }

}
