package org.intermine.webservice.server.lists;

import java.util.List;

import org.intermine.api.profile.InterMineBag;

/**
 * The common interface for formatters that know how to format lists.
 * @author Alex Kalderimis
 *
 */
public interface ListFormatter
{

    /**
     * Format a list into a list of string values.
     * @param list The list to format
     * @return A list of strings.
     */
    List<String> format(InterMineBag list);

    void setSize(int size);


}
