package wormbase.model.parser;
/*
 * 
 */
import java.io.*;
import java.util.*;

/**
 *  This class handles the parsing of the flat files into data structures
 *  for individual processing.
 *  
 *  Sample use:
 *
 *  FileParser fp = new FileParser(jaceFile);
 *	
 *	String[] dataObj;
 *	// foreach ""-separated paragraph in jacefile
 *	while( (dataObj = fp.getDataObj()) != null ){ 
 *		// do something
 *	}
 *
 */
public class FileParser {

	private MyBufferedReader inputStream;
	private int currentLine = 0;
	
	
	/**
	 * Creates a FileParser for given input file.
	 * @param inputFile Path to input file 
	 * @throws IOException 
	 */
	public FileParser(String inputFile) throws IOException {
 		inputStream = new MyBufferedReader(new FileReader(inputFile));
	}
	
	public FileParser(Reader reader) throws IOException {
 		inputStream = new MyBufferedReader(reader);
	}
	
	
	
	/**
	 * Returns multiline chunks of text separated by newlines.
	 * @return Array of strings comprising the Ace data object
	 * @throws IOException  
	 */
	public String[] getDataObj() throws IOException{
		if( inputStream.isStreamClosed() ){
			return null;
		}
		
		
		ArrayList<String> lines = new ArrayList<String>();
		
		String line = null;
		boolean startedObj = false; // Switched if non-whitespace passed in 
        try {
			while ((line = (String) inputStream.readLine()) != null) {
			    currentLine++;
			    //System.out.print("*"); // DEBUG
			    
			    if(line.equals("")){
			    	if(startedObj){
			    		break;
			    	}else{
			    		continue;
			    	}
			    }else if(startedObj != true){
			    	startedObj = true;
			    }
			    lines.add(line);
			}
		} finally {
            if (line == null) {
            	inputStream.close();
            	
            }
            
		}

		return lines.toArray(new String[lines.size()]);
	}
	
	/**
	 * Wrapper for getDataObj(), concatenates each string instead of returning
	 * an array of strings
	 * @return 
	 * @throws IOException 
	 */
	public String getDataString() throws IOException{
		String[] lines = getDataObj();
		if(lines == null || lines.length == 0){
			return null;
		}
		
		String resultLine = "";
		String separator = "";
		for(int i=0; i<lines.length; i++){
		    resultLine = resultLine + separator + lines[i];  
		}
		
		return resultLine;
	}
	
	/**
	 * Applies command to inputStream global variable
	 * @param command only "close" accepted
	 * @throws IOException 
	 */
	public int streamCmd(String command) throws IOException{
		if(command.equals("close")){
			inputStream.close();
			return 1;
		}else{
			return -1;
		}
	}
	
	public int getCurrentLine(){
		return currentLine;
	}
}
