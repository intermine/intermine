package org.intermine.web.security;

import java.security.PublicKey;

public interface KeyDecoder {
	
	PublicKey decode(String input) throws DecodingException;

}
