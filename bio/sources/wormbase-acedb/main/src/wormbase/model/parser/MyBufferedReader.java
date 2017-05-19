/**
 */
package wormbase.model.parser;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * @author jwong
 * A wrapper for the BufferedReader class designedh to now
 * throw an exception when tested for closed status.
 *
 */
public class MyBufferedReader extends BufferedReader {

	private boolean isStreamClosed; 
	/**
	 * @param in
	 */
	public MyBufferedReader(Reader in) {
		super(in);
		isStreamClosed = false;
	}

	/**
	 * @param in
	 * @param sz
	 */
	public MyBufferedReader(Reader in, int sz) {
		super(in, sz);
		isStreamClosed = false;
	}

	public String readLine() throws IOException {
		String line = super.readLine();
		return line;
	}
            
//	public void reset() throws IOException {
//		super.reset();
//		isStreamClosed = false;
//	}
	
	public void close() throws IOException {
		super.close();
		isStreamClosed = true;
	}
	
	public boolean isStreamClosed(){
		return isStreamClosed;
	}
	
	
	// TODO REDEFINE OTHER SUPER METHODS TO THROW EXCEPTION
	
}
