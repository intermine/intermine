// =====================================================================================================
// Class:  TmpProteinStroe
// Work:   Creates a Connection to the psql Database an provides methods to read Proteins out of the 
//         Table tbl_protein.
// Author: Dominik Grimm and Michael Menden
// =====================================================================================================

package org.intermine.bio.dataconversion;

import java.sql.*;

public class TmpProteinStore {
	
	// important for the Databaseconnection
	private String serverName;
	private String databaseName;
	private String user;
	private String password;
	private String url;	
	private Connection db;


    public TmpProteinStore()
    // ============================================================================================
    // This constructor should use the information from the unimine.properties file
    // to create the databaseconnection
    // ============================================================================================
    {
             PropertiesReader pr = new PropertiesReader("/home/dg353/unimine.properties.test");
             serverName   =  pr.getRhs("db.common-tmp.datasource.serverName");
             databaseName =  pr.getRhs("db.common-tmp.datasource.databaseName");
             user         =  pr.getRhs("db.common-tmp.datasource.user");
             password     =  pr.getRhs("db.common-tmp.datasource.password");
             url          =  getUrl(serverName, databaseName);
    }

	public void connectDb()
        // ============================================================================================
        // Creates a Databaseconnection and store in the private field db
        // ============================================================================================
	{
                try{
                        Class.forName("org.postgresql.Driver");
                        db = DriverManager.getConnection(url, user, password);
                } catch(Exception e){
                        e.printStackTrace();
                }
	}

	public void closeDb()
        // ============================================================================================
        // Close the Databaseconnection
        // ============================================================================================
	{
               try{
                        db.close();
                } catch(Exception e){
                        e.printStackTrace();
                }
	}

	public String getProtein(String accession)
        // ============================================================================================
        // Need the accesion id to search for the corresponding protein and return it.
        // ============================================================================================
	{
		String result = null;
		String query = "SELECT * FROM tbl_protein WHERE accession = '" + accession + "';";
		try{
			Statement st = db.createStatement();
			ResultSet rs = st.executeQuery(query);
			rs.next();
			result = rs.getString("protein");
		} catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}


	private String getUrl(String serverName, String databaseName)
        // ============================================================================================
        // Creates the right url for a connection to a posgresql database
        // ============================================================================================
	{
		return "jdbc:postgresql://" + serverName + "/" + databaseName;
	}

}
