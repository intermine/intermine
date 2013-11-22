package org.intermine.webservice.server.lists;

import java.util.Set;

import org.intermine.api.profile.Profile;

public interface ListMaker<T> {

    public T getInput();

    /**
     * Make the list requested by the user.
     * @param input The parsed parameter input.
     * @param type The type of the new list.
     * @param profile The profile to save the list in.
     * @param temporaryBagNamesAccumulator The accumulator to store the list of bags to delete.
     * @throws Exception If something goes wrong.
     */
    public abstract void makeList(T input, String type, Profile profile,
            Set<String> temporaryBagNamesAccumulator) throws Exception;

}
