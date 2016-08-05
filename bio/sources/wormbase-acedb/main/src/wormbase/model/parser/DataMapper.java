/**
 * 
 */
package wormbase.model.parser;

import java.io.*;
import java.util.Properties;

/**
 * @author jwong
 * Handles data mapping between AcdDB XML and InterMine with a properties file
 */
public class DataMapper extends Properties {

	/*
	 * Holds fields included per data type in the style: [Type][Attributes]
	 * Example: 
	 * 	{ 
	 * 		"Gene", {"primaryIdentifier", "Organism.name"} 
	 * 		".... to be continued	
	 * 	}
	 */
	private String typeMatrix[][];
	
	/**
	 * 
	 */
	public DataMapper() {
		super();
	}

	/**
	 * @param defaults
	 */
	public DataMapper(Properties defaults) {
		super(defaults);
		
	}
	
	public void load(Reader reader) throws IOException{
		super.load(reader);
	}

}
