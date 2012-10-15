package org.intermine.webservice.server.lists;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

public class ListShareCreationService extends JSONService {

	private final ProfileManager pm;
	private final SharedBagManager sbm;

	public ListShareCreationService(InterMineAPI im) {
		super(im);
		pm = im.getProfileManager();
		sbm = SharedBagManager.getInstance(pm);
	}
	
	private final class UserInput {
		final Profile owner;
		final InterMineBag bag;
		final String recipient;
		
		UserInput() throws ServiceException {
			owner = getPermission().getProfile();
			if (!owner.isLoggedIn()) {
				throw new ServiceForbiddenException("Not authenticated.");
			}
			Map<String, InterMineBag> bags = owner.getSavedBags();
			if (bags.isEmpty()) {
				throw new BadRequestException("You do not have any lists to share");
			}
			String bagName = request.getParameter("list");
			if (isBlank(bagName)) {
				throw new BadRequestException("Missing parameter: 'list'");
			}
			bag = bags.get(bagName);
			if (bag == null) {
				throw new ResourceNotFoundException("The value of the 'list' parameter is not the name of a list you own");
			}
			recipient = request.getParameter("with");
			if (isBlank(recipient)) {
				throw new BadRequestException("Missing parameter: 'with'");
			}
			// Is this dangerous? it allows users to
			// scrape for usernames. But the you can do the same
			// thing in the registration service...
			if (pm.getProfile(recipient) == null) {
				throw new ResourceNotFoundException("The value of the 'recipient' parameter is not the name of user you can share lists with");
			}
		}
	}

	@Override
	public String getResultsKey() {
		return "share";
	}
	
	@Override
	protected void execute() throws ServiceException {
		 UserInput input = new UserInput();
		 
		 sbm.shareBagWithUser(input.bag, input.recipient);
		 
		 Map<String, Object> data = new HashMap<String, Object>();
		 data.put(input.bag.getName(), sbm.getUsersWithAccessToBag(input.bag));
		 
		 addResultItem(data, false);
	}

}
