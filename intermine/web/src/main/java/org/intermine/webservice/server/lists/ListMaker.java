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

import java.util.Set;

import org.intermine.api.profile.Profile;

/**
 * The type of things that can make lists.
 * @author Alex Kalderimis
 *
 * @param <T> The kind of input they need.
 */
public interface ListMaker<T>
{

    /** @return The input. **/
    T getInput();

    /**
     * Make the list requested by the user.
     * @param input The parsed parameter input.
     * @param type The type of the new list.
     * @param profile The profile to save the list in.
     * @param temporaryBagNamesAccumulator The accumulator to store
     *         the list of bags to delete.
     * @throws Exception If something goes wrong.
     */
    void makeList(T input, String type, Profile profile,
            Set<String> temporaryBagNamesAccumulator) throws Exception;

}
