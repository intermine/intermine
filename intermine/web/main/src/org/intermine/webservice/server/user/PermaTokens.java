package org.intermine.webservice.server.user;

import java.util.HashMap;
import java.util.Map;

import org.intermine.model.userprofile.PermanentToken;
import org.intermine.webservice.server.core.ISO8601DateFormat;

public final class PermaTokens {

	public static Map<String, Object> format(PermanentToken token) {
		Map<String, Object> map = new HashMap<String, Object>();
        map.put("token", token.getToken());
        map.put("message", token.getMessage());
        map.put("dateCreated", ISO8601DateFormat.getFormatter().format(token.getDateCreated()));
        return map;
	}
}
