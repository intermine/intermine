package org.intermine.task;

public abstract class DBDirectDataLoaderTask extends DirectDataLoaderTask {
  
  protected String dbAlias;
  protected String clsName;

  /**
   * Set the source specific subclass of FileConverter to run
   * @param clsName name of converter class to run
   */
  public void setClsName(String clsName) {
      this.clsName = clsName;
  }

  /**
   * Set the Database to read from
   * @param dbAlias the database alias
   */
  public void setDbAlias(String dbAlias) {
      this.dbAlias = dbAlias;
  }
}
