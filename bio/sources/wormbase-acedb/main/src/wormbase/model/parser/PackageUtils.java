/**
 * 
 */
package wormbase.model.parser;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
/**
 * @author jwong
 *
 */
public class PackageUtils {
	
//	public static Document loadXMLFrom(String xml)
//	    throws SAXException, IOException {
//	    ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
//	    
////	    StringBuilder sb = new StringBuilder();
////	    int i;
////	    while( ( i = is.read() ) != -1 ){
////	    	sb.append( (char) i );
////	    }
////	    
////	    
////	    String str = sb.toString();
////	    
////	    return null;
//	    
//	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//	    factory.setNamespaceAware(true);
//	    
//	    DocumentBuilder builder = null;
//	    try {
//	        builder = factory.newDocumentBuilder();
//	    }
//	    catch (ParserConfigurationException ex) {
//	    }  
//	    Document doc = builder.parse(is);
//	    is.close();
//	    return doc;
//
//	}

	public static Document loadXMLFrom(String xml)
		    throws SAXException, java.io.IOException {
		    return loadXMLFrom(new java.io.ByteArrayInputStream(xml.getBytes()));
		}
	
	public static Document loadXMLFrom(InputStream is) 
	    throws SAXException, IOException {
	    DocumentBuilderFactory factory =
	        DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    DocumentBuilder builder = null;
	    try {
	        builder = factory.newDocumentBuilder();
	    }
	    catch (ParserConfigurationException ex) {
	    }  
	    Document doc = builder.parse(is);
	    is.close();
	    return doc;
	}
	
	/**
	 * Removes numbers from the beginnings of XML tags in the string.
	 * Ex: <2_point> -> <two_point>
	 * @param xml
	 * @return
	 * @throws Exception 
	 */
	public static String sanitizeXMLTags(String xml) throws Exception{
		String repairedXML = xml;
		
		
		repairedXML = replaceAmpersands(repairedXML);
		repairedXML = replaceAngleBrackets(repairedXML);
		repairedXML = replaceXMLNumberTags(repairedXML);
		
		
		
		return repairedXML;
	}
	
	private static String replaceXMLNumberTags(String xml){
		HashMap<Integer, String> numberMap = new HashMap<Integer, String>();
		numberMap.put(1, "one");
		numberMap.put(2, "two");
		numberMap.put(3, "three");
		numberMap.put(4, "four");
		numberMap.put(5, "five");
		numberMap.put(6, "six");
		numberMap.put(7, "seven");
		numberMap.put(8, "eight");
		numberMap.put(9, "nine");
		numberMap.put(0, "zero");

		String patternStr = "(</?)(\\d)";
		
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(xml);
		StringBuffer sb = new StringBuffer();
		while(matcher.find()){
			String digit = matcher.group(2);
			String processedText = numberMap.get(Integer.parseInt(digit));
			matcher.appendReplacement(sb, matcher.group(1)+processedText);
		}
		matcher.appendTail(sb);

		
		return sb.toString();

	}
	
	/**
	 * Replaces all instances of left or right angle brackets between tags
	 * containing 1 to 18 characters
	 * @param xml
	 * @return
	 * @throws Exception 
	 */
	private static String replaceAngleBrackets(String xml) throws Exception{
		HashMap<String, String> replacement = new HashMap<String, String>();
		replacement.put("<", "&lt;");
		replacement.put(">", "&gt;");

		// Non-greedily matches any character string between <Text> and any tag
		// between 1 and 18 characters in it.
		String patternStr = "(<Text>)(.+?)(</?[^<>@]{1,18}>)";
		
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(xml);
		StringBuffer sb = new StringBuffer();
		while(matcher.find()){
			String text = matcher.group(2);
			String processedText = replaceWithMappedValue(text, "[<>]", replacement);
			matcher.appendReplacement(sb, 
					matcher.group(1) + processedText + matcher.group(3));
		}
		matcher.appendTail(sb);

		
		return sb.toString();

	}
	
	private static String replaceAmpersands(String xml){
		HashMap<Character, String> replacement = new HashMap<Character, String>();
		replacement.put('&', "&amp;");

		String patternStr = "(&)";
		
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(xml);
		StringBuffer sb = new StringBuffer();
		while(matcher.find()){
			String digit = matcher.group(1);
			String processedText = replacement.get(digit.charAt(0));
			matcher.appendReplacement(sb, processedText);
		}
		matcher.appendTail(sb);

		
		return sb.toString();

	}
	
	// Only replaces a single angle bracket found between two tags
//	private static String replaceInvalidAngleBrackets(String xml){
//		HashMap<Character, String> replacement = new HashMap<Character, String>();
//		replacement.put('<', "&lt;");
//		replacement.put('>', "&gt;");
//		
//		String patternStr = "(>[^<>]+)(<|>)([^<>]+<)";
//		
//		Pattern pattern = Pattern.compile(patternStr);
//		Matcher matcher = pattern.matcher(xml);
//		StringBuffer sb = new StringBuffer();
//		while(matcher.find()){
//			String match = matcher.group(2);
//			String processedText = replacement.get(match.charAt(0));
//			matcher.appendReplacement(sb, 
//					matcher.group(1) + processedText + matcher.group(3));
//		}
//		matcher.appendTail(sb);
//
//		
//		return sb.toString();
//
//	}
	
	/**
	 * Replaces each occurrence of the matched pattern with the corresponding key
	 * value from the passed in map
	 * @param input String to be modified
	 * @param patternStr Regular expression pattern used to match keys.  Must not 
	 * 	contain parameterized match groups.
	 * @param replacement The string mapping coordinating replacement.
	 * @return
	 * @throws Exception 
	 */
	public static String replaceWithMappedValue(String input, String patternStr, 
			HashMap<String, String> replacement) throws Exception{
		
		Pattern pattern = Pattern.compile("("+patternStr+")");
		Matcher matcher = pattern.matcher(input);
		StringBuffer sb = new StringBuffer();
		while(matcher.find()){
			String match = matcher.group(1);
			if(replacement.containsKey(match)){
				match = replacement.get(match);
			}else{
				throw new Exception("ERROR: ["+match+"] is not a key in ["+
						replacement.toString()+"]");
			}
			
			matcher.appendReplacement(sb, match);
		}
		matcher.appendTail(sb);

		
		return sb.toString();

	}
}
