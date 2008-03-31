// =====================================================================================================
// Class: PropertiesReader
// Author: Dominik Grimm and Michael Menden
// =====================================================================================================
package org.intermine.bio.dataconversion;

import java.io.*;

public class PropertiesReader {

	private String file;   // Path to the file

	public PropertiesReader(String file){
		this.file = file;
	}

	//just for testing!!!
	public static void main (String args[])
	{	
		PropertiesReader test = new PropertiesReader("/home/mm719/unimine.properties.test");
		System.out.println("serverName: " + test.getRhs("db.common-tmp.datasource.serverName"));
                System.out.println("databaseName: " + test.getRhs("db.common-tmp.datasource.databaseName"));
                System.out.println("user: " + test.getRhs("db.common-tmp.datasource.user"));
                System.out.println("password: " + test.getRhs("db.common-tmp.datasource.password"));
	}	



	public String getRhs(String lhs){
	// ==============================================================================================
	// Search in a file for the first eqation with an equal lhs (left hsnd side)
	// This Method return then the rhs (right hand side)
	// ==============================================================================================
                try
                {
                        // Open the properties file
                        FileInputStream fin = new FileInputStream (file);
                        DataInputStream dis = new DataInputStream(new BufferedInputStream(fin));

                        while (dis.available() != 0) {
                                String tmp = dis.readLine();
                                if(tmp.length() > lhs.length()
                                   && tmp.substring(0,lhs.length()).compareTo(lhs) == 0){
                  			return tmp.substring(lhs.length()+1,tmp.length());
				}
			}
                        // Close our input streams
                        fin.close();
                        dis.close();
                }catch (IOException e){
                        e.printStackTrace();
                }
		return null;
	}

}
