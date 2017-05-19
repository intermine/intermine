package org.intermine.bio.dataconversion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingFileKey {

	Pattern paransB4XPath;
	String castType = null;
	String dataPath = null;
	String rawKey	= null;
	
	public MappingFileKey(String mappingFileKey) {
		rawKey = mappingFileKey;
		paransB4XPath  = Pattern.compile("\\((.*)\\)\\s*");
        Matcher typeCastMatcher = paransB4XPath.matcher(mappingFileKey);
     	if(typeCastMatcher.find()){
    		String matchedText = typeCastMatcher.group(1);
    		if( matchedText != null && matchedText.length()!=0 ){
    			castType = matchedText;
    		}
	     	dataPath = mappingFileKey.substring(typeCastMatcher.end());
    	}else{
    		dataPath = mappingFileKey;
    	}
	}
	
    /**
     * Gets cast type from property key if exists aka: (Gene)Datapath
     * @param propKey
     * @return cast type, null if nonexistent
     */
	public String getCastType(){
		return castType;
	}
	
	public String getDataPath(){
		return dataPath;
	}
	
	public String getRawKey(){
		return rawKey;
	}
	
	public boolean equals(MappingFileKey m){
		if(this.getRawKey() == m.getRawKey()){
			return true;
		}else{
			return false;
		}
	}

}
