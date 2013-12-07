package org.intermine.webservice.server.user;

import java.util.Collections;

import org.intermine.api.InterMineAPI;
import org.intermine.model.userprofile.PermanentToken;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

public class PermaTokenInfoService extends ReadWriteJSONService {

	private String uuid;

	public PermaTokenInfoService(InterMineAPI im, String uuid) {
		super(im);
		this.uuid = uuid;
	}

	@Override
	protected void execute() throws Exception {
		PermanentToken token = new PermanentToken();
		token.setToken(uuid);
		token = (PermanentToken) im.getProfileManager()
				  .getProfileObjectStoreWriter()
				  .getObjectByExample(token, Collections.singleton("token"));
		if (token == null) {
			throw new ResourceNotFoundException(uuid + " is not a token");
		}
		addResultItem(PermaTokens.format(token), false);
	}

	@Override
	public String getResultsKey() {
		return "token";
	}

}
