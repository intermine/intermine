package org.intermine.webservice.server.lists;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.collections.TransformerUtils.invokerTransformer;
import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.JSONFormatter;

public class ListTagService extends AbstractListService {

    public ListTagService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = super.getHeaderAttributes();
        attributes.put(JSONFormatter.KEY_INTRO, "\"tags\":[");
        attributes.put(JSONFormatter.KEY_OUTRO, "]");
        attributes.put(JSONFormatter.KEY_QUOTE, true);
        return attributes;
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String listName = request.getParameter("name");
        if (StringUtils.isBlank(listName)) {
            throw new BadRequestException("Required parameter 'name' is blank");
        }
        BagManager bagManager = im.getBagManager();
        Profile profile = SessionMethods.getProfile(request.getSession());
        Map<String, InterMineBag> lists = bagManager.getUserAndGlobalBags(profile);
        InterMineBag list = lists.get(listName);
        if (list == null) {
            throw new ResourceNotFoundException("You do not have access to a list called " + listName);
        }
        List<Tag> tags = bagManager.getTagsForBag(list);
        List<String> tagNames = (List<String>) collect(tags, invokerTransformer("getTagName"));
        output.addResultItem(tagNames);
    }

}
