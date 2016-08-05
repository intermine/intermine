package wormbase.model.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.*;

public class ModelParser {

	private FileParser fp;
	
	/**
	 * @throws IOException 
	 * 
	 */
	public ModelParser(String modelFile) throws IOException{
		fp = new FileParser(modelFile);
		showOff();
	}
	
	public ModelParser(Reader reader) throws IOException{
		fp = new FileParser(reader);
		showOff();
	}

	public void showOff() throws IOException{
			String[] dataObj;
			
			// Get the gene model specification
			while( (dataObj = fp.getDataObj()) != null ){ 
				System.out.println("JDJDJD:: "+dataObj[0]);
			}
		
	}
	
	

}
