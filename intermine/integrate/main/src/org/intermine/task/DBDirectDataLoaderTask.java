package org.intermine.task;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.tools.ant.BuildException;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

public abstract class DBDirectDataLoaderTask extends DirectDataLoaderTask {
  
  private String dbName;
  private Connection connection;
  private Database db = null;
  
  protected Database getDb() {
    if (db==null) {
      if (dbName == null ) {
        throw new BuildException("dbName is not set.");
      }
      try {
        db = DatabaseFactory.getDatabase(dbName);
      } catch (ClassNotFoundException | SQLException e) {
        throw new BuildException("Trouble getting database: " +e.getMessage());
      }     
    }
    return db;
  }
  
  protected Connection getConnection() {
    if (connection == null ) {
      try {
        connection = getDb().getConnection();
      } catch (SQLException e) {
        throw new BuildException("Trouble getting database connection: " +e.getMessage());
      }
    }
    return connection;
  }
  
  /**
   * Set the Database to read from
   * @param dbName the database alias
   */
  
  public void setSourceDbName(String dbName) {
    this.dbName = dbName;
  }
}
