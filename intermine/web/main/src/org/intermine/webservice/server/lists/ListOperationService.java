package org.intermine.webservice.server.lists;

import java.util.Arrays;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;

public abstract class ListOperationService extends ListMakerService {

    public ListOperationService(InterMineAPI api) {
        super(api);
    }

    protected abstract int doOperation(ListInput input, String type, Profile profile,
            Set<String> temporaryBagsAccumulator) throws Exception;


    @Override
    protected void makeList(ListInput input, String type, Profile profile,
        Set<String> rubbishbin) throws Exception {
        int size = doOperation(input, type, profile, rubbishbin);
        InterMineBag newList;
        if (size == 0) {
            output.addResultItem(Arrays.asList("0"));
            newList = profile.createBag(
                input.getTemporaryListName(), type, input.getDescription(), im.getClassKeys());
        } else {
            newList = profile.getSavedBags().get(input.getTemporaryListName());
            if (input.getDescription() != null) {
                newList.setDescription(input.getDescription());
            }
            output.addResultItem(Arrays.asList("" + newList.size()));
        }
        if (input.doReplace()) {
            ListServiceUtils.ensureBagIsDeleted(profile, input.getListName());
        }
        if (!input.getTags().isEmpty()) {
            bagManager.addTagsToBag(input.getTags(),
                    profile.getSavedBags().get(input.getTemporaryListName()),
                    profile);
        }
        profile.renameBag(input.getTemporaryListName(), input.getListName());

    }

}
