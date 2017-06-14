package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.List;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStoreException;

/**
 * A formatter that represents lists as HTML.
 * @author Alex Kalderimis
 *
 */
public class HtmlListFormatter implements ListFormatter
{

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
