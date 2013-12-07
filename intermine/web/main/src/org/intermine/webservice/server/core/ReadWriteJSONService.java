package org.intermine.webservice.server.core;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

// Convenience for services that need to combine JSON service behaviour
// with strict RW authorisation. 
public abstract class ReadWriteJSONService extends JSONService {

	private static final String DENIAL_MSG = "Access denied.";

	public ReadWriteJSONService(InterMineAPI im) {
		super(im);
	}

	@Override
	protected void validateState() {
	    if (!isAuthenticated() || getPermission().isRO()) {
	        throw new ServiceForbiddenException(DENIAL_MSG);
	    }
	}

}