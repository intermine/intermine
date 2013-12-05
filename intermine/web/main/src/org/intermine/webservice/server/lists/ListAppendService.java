package org.intermine.webservice.server.lists;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

public class ListAppendService extends ListUploadService {

    public static final String USAGE =
          "List Append Service\n"
        + "===================\n"
        + "Append items to a list\n"
        + "Parameters:\n"
        + "name: the name of the list to append items to\n\n"
        + "Content: text/plain - list of ids\n";

    public ListAppendService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void makeList(
            ListInput listInput,
            String type,
            Profile profile,
            Set<String> rubbishbin) throws Exception {

        Set<String> ids = new LinkedHashSet<String>();
        Set<String> unmatchedIds = new HashSet<String>();

        ListCreationInput input = (ListCreationInput) listInput;

        InterMineBag bag = profile.getSavedBags().get(input.getListName());
        if (bag == null) {
            throw new ServiceForbiddenException(
                input.getListName() + " is not a list you have access to");
        }

        processIdentifiers(bag.getType(), input, ids, unmatchedIds, bag);

        setListSize(bag.size());

        for (Iterator<String> i = unmatchedIds.iterator(); i.hasNext();) {
            List<String> row = new ArrayList<String>(Arrays.asList(i.next()));
            if (i.hasNext()) {
                row.add("");
            }
            output.addResultItem(row);
        }

    }

}
