/**
 * 
 */
package wormbase.model.parser;

/**
 * @author jwong
 * Represents a parsed Wormbase model schema of a certain class.  
 */
public class ModelClass {

	private String name;
	
	public ModelClass(String _name){
		name = _name;
		
	}
	
	public String getName(){
		return name;
	}
	
}
