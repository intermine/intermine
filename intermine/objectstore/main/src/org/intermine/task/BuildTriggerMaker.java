package org.intermine.task;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.sql.DatabaseUtil;

/* Code for generating SQL files for manual CRUD operations on InterMine database.
 * Sometimes, after a long loading process, you see that something is not right: either
 * typo in a name, or a major issue such as an entire dataset in error. Rather than
 * starting over from scratch, you'd like to be able to make a small change. The SQL
 * files generated from this task will allow you to do that.
 * 
 * To use, build the SQL files with the ant task "ant build-sql-triggers" from the dbmodel
 * subdirectory (after the build-db task).
 * There will be 2 files TriggerMaker.sql and TriggerRemover.sql in build/model/
 * Connect to postgreSQL using psql and read the command file
 * 
 * \i build/model/TriggerMaker.sql
 * 
 * You can now do basic create/update/delete operations such as:
 * 
 * UPDATE organism set genus='Homo" where genus='Homer';
 * DELETE FROM organism where commonname='yeti';
 * 
 * And the operations are propagated to the superclass and intermineobject tables. If you
 * have not done so, familiarlize yourself with the structure of the tables the database.
 * 
 * Tables have default values supplied for id and class, so it is possible to create
 * new records
 * 
 * INSERT INTO organism (genus,species) values ('Hello','world');
 * 
 * The id is supplied from a sequence im_serial which is initially set to the
 * maximum id of intermineobject.
 * 
 * At the completion of the manual operations, remove all triggers and stored
 * procedures with
 * 
 * \i build/model/TriggerRemover.sql
 * 
 * What the triggers and procedures do NOT do:
 * 
 * 1) Foreign key constraints are not enforced. If you delete a gene, there may still
 * entries in the genesproteins table or a reference to this from the geneid field in
 * the mrna table. Foreign keys are enforced at the application layer. This means whoever
 * is doing the update needs to keep things straight. (This is possible to implement.
 * It may be done in the future.)
 * 
 * 2) The tracker table is not updated. If you do an integration step after manual
 * operations and the integrator is trying to update a column value that you inserted
 * manually, the integration step will fail.
 * 
 * 3) The clob table cannot be manipulated. Again, this may also be changed in the future.
 * 
 * Other requirements:
 * 1) plpgsql must be installed in your postgres (select * from pg_language where lanname='plpgsql';)
 * Check the postgreSQL manuals for instructions on installing languages if needed.
 * 2) Backup the database prior to making changes, especially if there are changes that affect
 * foreign keys.
 * 3) Be sure to run TriggerRemover.sql AND verify it was successful before resuming normal
 * InterMine processing. All triggers and stored procedures are prefixed with 'im_'
 * 
 * This is an experimental feature. Use at your own risk.
 */

public class BuildTriggerMaker extends Task {

  /*
   * If the auto-generated name for the triggers and stored procedures gets too
   * long, we may need to abbreviate them. This keeps track of the shortened name. 
   */
  static HashMap<String,String> abbreviations = new HashMap<String,String>();
  protected File destDir;
  protected String osname;

  /**
   * Sets the destination directory for the SQL files.
   * @param destDir the destination directory
   */
  public void setDestDir(File destDir) {
    this.destDir = destDir;
  }

  /**
   * Sets the os alias
   * @param osname the os alias
   */
  public void setOsName(String osname) {
    this.osname = osname;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void execute() {
    if (destDir == null) {
      throw new BuildException("destDir attribute is not set");
    }
    if (osname == null) {
      throw new BuildException("osname attribute is not set");
    }

    try {

      ObjectStore os;
      try {
        os = ObjectStoreFactory.getObjectStore(osname);
      } catch (Exception e) {
        throw new RuntimeException("Cannot connect to objectstore: "+ e.getMessage());
      }
      Model model = os.getModel();
      String makerFileName = "TriggerMaker.sql";
      String removerFileName = "TriggerRemover.sql";

      FileWriter makerW;
      FileWriter removerW;
      try {
        makerW = new FileWriter(new File(destDir,makerFileName));
        removerW = new FileWriter(new File(destDir,removerFileName));
      } catch (IOException e) {
        throw new BuildException("Cannot open SQL file: "+ e.getMessage());
      }
      PrintWriter makerPW = new PrintWriter(makerW);
      PrintWriter removerPW = new PrintWriter(removerW);
      
      makerPW.print(writeDisclaimer());
      removerPW.print(removeDisclaimer());

      makerPW.print(writeSequence());
      removerPW.print(removeSequence());
      for(ClassDescriptor cld : model.getBottomUpLevelTraversal()) {

        if (!cld.getUnqualifiedName().equals("InterMineObject") &&
            cld.getFieldDescriptorByName("id") != null ) {

          makerPW.print(writeDefaultClassConstraint(cld));
          removerPW.print(removeDefaultClassConstraint(cld));
          Set<ClassDescriptor> sCDs = cld.getSuperDescriptors();

          for(ClassDescriptor superCld : sCDs) {
            if( !superCld.getUnqualifiedName().equals("InterMineObject")) {
              makerPW.print(writeSuperClassActions(cld,superCld));
              removerPW.print(removeSuperClassActions(cld,superCld));
            }
          }    
          makerPW.print(writeInterMineObjectActions(cld));
          removerPW.print(removeInterMineObjectActions(cld));
        }
      }
      makerPW.print(writeDisclaimer());
      makerPW.close();
      removerPW.print(removeDisclaimer());
      removerPW.close();
    } catch (Exception e) {
      throw new BuildException("Something bad happened: "+e.getMessage());
    }
  }

  static private String getDBName(String word) {
    // attribute names may need some rewriting to become good
    // column names. 'Class' is an exception. That is considered a SQL
    // reserved word, but we use it as a column name.
    if (word.equals("class")) return word;
    return DatabaseUtil.generateSqlCompatibleName(word);
  }

  static private String shortName(String table) {
    /* since we can run into trouble if the length of the function or trigger
     * name exceeds a certain length, let's truncate when we have to.
     * The trigger name can be 63 characters ("show max_identifier_length;")
     * We'll abbreviate things longer than 20 and append a new letter 
     * if we have to. That gives some room.
     * 
     * This algorithm is pretty crude. But I'm hoping it's "good enough"
     */ 
    if (table.length() < 20) {
      return table;
    }
    // we're more than 20 characters. Take the first 19 and start appending
    // a new letter.
    StringBuffer trialWord = new StringBuffer(table.substring(0,19));
    trialWord.append("A");
    while (abbreviations.containsKey(trialWord) && !abbreviations.get(trialWord).equals(table) ) {
      char newChar = trialWord.charAt(20);
      if (newChar == 'Z') {
        System.err.println("There are too many tables starting with the first 19 characters!");
        System.exit(1);
      }
      newChar++;
      trialWord.setCharAt(20,newChar);
    }
    abbreviations.put(trialWord.toString(),table);
    return trialWord.toString();

  }

  static private String writeInterMineObjectActions(ClassDescriptor c) {
    String tableName = getDBName(c.getUnqualifiedName());
    String cmds =
        "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_InterMineObject_UPD_tg ON "+tableName+";\n" +
            makeUpdateInterMineObjectBody(c) +
            "CREATE TRIGGER im_"+shortName(tableName)+"_InterMineObject_UPD_tg AFTER UPDATE ON "+
            tableName + " FOR EACH ROW EXECUTE PROCEDURE "+
            "im_"+shortName(tableName)+"_InterMineObject_UPD();\n\n" +

              "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_InterMineObject_INS_tg ON "+tableName+";\n" + 
              makeInsertInterMineObjectBody(c) +
              "CREATE TRIGGER im_"+shortName(tableName)+"_InterMineObject_INS_tg AFTER INSERT ON "+
              tableName + " FOR EACH ROW EXECUTE PROCEDURE "+
              "im_"+shortName(tableName)+"_InterMineObject_INS();\n\n" +

              "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_InterMineObject_DEL_tg ON "+tableName+";\n" +
              makeDeleteInterMineObjectBody(c) + 
              "CREATE TRIGGER im_"+shortName(tableName)+"_InterMineObject_DEL_tg AFTER DELETE ON "+
              tableName + " FOR EACH ROW EXECUTE PROCEDURE "+
              "im_"+shortName(tableName)+"_InterMineObject_DEL();\n\n";

    return cmds;
  }

  static private String makeUpdateInterMineObjectBody(ClassDescriptor c) {
    String tableName = getDBName(c.getUnqualifiedName());
    StringBuffer body = new StringBuffer("CREATE OR REPLACE FUNCTION im_" + shortName(tableName) +
        "_InterMineObject_UPD() RETURNS TRIGGER AS $$\n");
    body.append("DECLARE objectText TEXT;\n");
    body.append("BEGIN\n");
    body.append("IF ( NEW.class != '"+c.getName()+"' ) THEN RETURN NULL; END IF;\n");
    Set<FieldDescriptor> classFD = c.getAllFieldDescriptors();
    body.append("objectText = '$_^"+c.getName()+"';\n");
    for( FieldDescriptor fD: classFD) {
      if (fD.isAttribute() && !fD.getName().equals("id")) {
        body.append("IF ( NEW." + getDBName(fD.getName()) + " IS NOT NULL ) THEN objectText := objectText || " +
            "'$_^a"+fD.getName()+"$_^'||NEW."+getDBName(fD.getName())+"; END IF;\n");
      } else if (fD.isReference() ) {
        body.append("IF ( NEW." + fD.getName() + "id IS NOT NULL ) THEN objectText := objectText || " +
            "'$_^r"+fD.getName()+"$_^'||NEW."+fD.getName()+"id; END IF;\n");
      }
    }
    body.append("objectText:=objectText||'$_^aid$_^'||NEW.id;\n");
    body.append("EXECUTE 'UPDATE intermineobject SET object=$1 WHERE id=$2 and class=$3' USING objectText,NEW.id,'" +
        c.getName()+"';\n");
    body.append("RETURN NEW;\nEND;\n $$ LANGUAGE plpgsql;\n\n");
    return body.toString();

  }

  static private String makeInsertInterMineObjectBody(ClassDescriptor c) {

    String tableName = getDBName(c.getUnqualifiedName());
    StringBuffer body = new StringBuffer("CREATE OR REPLACE FUNCTION im_" + shortName(tableName) +
        "_InterMineObject_INS() RETURNS TRIGGER AS $$\n");
    body.append("DECLARE objectText TEXT;\n");
    body.append("DECLARE objectid INT;\n");
    body.append("BEGIN\n");
    body.append("IF ( NEW.class != '"+c.getName()+"' ) THEN RETURN NULL; END IF;\n");
    Set<FieldDescriptor> classFD = c.getAllFieldDescriptors();
    body.append("objectText = '$_^"+c.getName()+"';\n");
    for( FieldDescriptor fD: classFD) {
      if (fD.isAttribute() && !fD.getName().equals("id") ) {
        body.append("IF ( NEW." + getDBName(fD.getName()) + " IS NOT NULL ) THEN objectText := objectText || " +
            "'$_^a"+fD.getName()+"$_^'||NEW."+getDBName(fD.getName())+"; END IF;\n");
      } else if (fD.isReference() ) {
        body.append("IF ( NEW." + fD.getName() + "id IS NOT NULL ) THEN objectText := objectText || " +
            "'$_^r"+fD.getName()+"$_^'||NEW."+fD.getName()+"id; END IF;\n");
      }
    }
    body.append("objectText:=objectText||'$_^aid$_^'||NEW.id;\n");
    body.append("objectId := NEW.id;\n");
    body.append("EXECUTE 'INSERT INTO intermineobject (id,class,object) values ($1,$2,$3)' USING objectId,'"+c.getName()+"',objectText;\n");
    body.append("RETURN NEW;\nEND;\n $$ LANGUAGE plpgsql;\n\n");
    return body.toString();  
  }

  static private String makeDeleteInterMineObjectBody(ClassDescriptor c) {

    String tableName = getDBName(c.getUnqualifiedName());
    StringBuffer body = new StringBuffer("CREATE OR REPLACE FUNCTION im_" + shortName(tableName) +
        "_InterMineObject_DEL() RETURNS TRIGGER AS $$\n");
    body.append("BEGIN\n");
    body.append("IF ( OLD.class != '"+c.getName()+"' ) THEN RETURN NULL; END IF;\n");
    body.append("DELETE FROM intermineobject WHERE id=OLD.id;\n");
    body.append("RETURN OLD;\n");
    body.append("END;\n $$ LANGUAGE plpgsql;\n\n");
    return body.toString();
  }

  static private String removeInterMineObjectActions(ClassDescriptor c) {
    String tableName = getDBName(c.getUnqualifiedName());
    String cmds =
        "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_IntermineObject_UPD_tg ON "+tableName+";\n"+
            "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_IntermineObject_INS_tg ON "+tableName+";\n"+
            "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_IntermineObject_DEL_tg ON "+tableName+";\n"+
            "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_IntermineObject_TRN_tg ON "+tableName+";\n"+
            "DROP FUNCTION IF EXISTS im_"+shortName(tableName)+"_IntermineObject_INS();\n"+
            "DROP FUNCTION IF EXISTS im_"+shortName(tableName)+"_IntermineObject_UPD();\n"+
            "DROP FUNCTION IF EXISTS im_"+shortName(tableName)+"_IntermineObject_DEL();\n"+
            "DROP FUNCTION IF EXISTS im_"+shortName(tableName)+"_IntermineObject_TRN();\n\n";
    return cmds;
  }


  static private String writeSuperClassActions(ClassDescriptor c, ClassDescriptor s) {
    String tableName = getDBName(c.getUnqualifiedName());
    String superTableName = getDBName(s.getUnqualifiedName());

    Set<FieldDescriptor> superFD = s.getAllFieldDescriptors();
    Set<FieldDescriptor> classFD = c.getAllFieldDescriptors();
    HashSet<String> commonFields = new HashSet<String>();
    commonFields.add("class");
    for(FieldDescriptor fD : superFD) {
      if (classFD.contains(fD)) {
        if (fD.isAttribute() ) {
          commonFields.add(fD.getName());
        } else if (fD.isReference() ) {
          commonFields.add(fD.getName()+"id");
        }
      }
    }


    String cmds =
        "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_"+shortName(superTableName)+"_UPD_tg ON " +
            tableName + ";\n" +
            makeUpdateBody(tableName,superTableName,commonFields) +
            "CREATE TRIGGER im_"+shortName(tableName)+"_"+shortName(superTableName)+"_UPD_tg AFTER UPDATE ON " +
            tableName + " FOR EACH ROW EXECUTE PROCEDURE " +
            "im_"+shortName(tableName)+"_"+shortName(superTableName)+"_UPD();\n" +

              "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_"+shortName(superTableName)+"_INS_tg ON " +
              tableName + ";\n" +
              makeInsertBody(tableName,superTableName,commonFields) +
              "CREATE TRIGGER im_"+shortName(tableName)+"_"+shortName(superTableName)+"_INS_tg AFTER INSERT ON "+
              tableName + " FOR EACH ROW EXECUTE PROCEDURE "+
              "im_"+shortName(tableName)+"_"+shortName(superTableName)+"_INS();\n" +

              "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_"+shortName(superTableName)+"_DEL_tg ON " +
              tableName + ";\n"+
              makeDeleteBody(tableName,superTableName,commonFields) +
              "CREATE TRIGGER im_"+shortName(tableName)+"_"+shortName(superTableName)+"_DEL_tg AFTER DELETE ON " +
              tableName + " FOR EACH ROW EXECUTE PROCEDURE "+
              "im_"+shortName(tableName)+"_"+shortName(superTableName)+"_DEL();\n";

    return cmds;
  }

  static private String removeSuperClassActions(ClassDescriptor c, ClassDescriptor s) {
    String tableName = getDBName(c.getUnqualifiedName());
    String superTableName = getDBName(s.getUnqualifiedName());

    String cmds =
        "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_"+shortName(superTableName)+"_INS_tg ON "+tableName+";\n"+
            "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_"+shortName(superTableName)+"_UPD_tg ON "+tableName+";\n"+
            "DROP TRIGGER IF EXISTS im_"+shortName(tableName)+"_"+shortName(superTableName)+"_DEL_tg ON "+tableName+";\n"+
            "DROP FUNCTION IF EXISTS im_"+shortName(tableName)+"_"+shortName(superTableName)+"_INS();\n"+
            "DROP FUNCTION IF EXISTS im_"+shortName(tableName)+"_"+shortName(superTableName)+"_UPD();\n"+
            "DROP FUNCTION IF EXISTS im_"+shortName(tableName)+"_"+shortName(superTableName)+"_DEL();\n\n";
    return cmds;
  }

  private static String makeUpdateBody(String table, String superTable, Set<String> fields) {
    StringBuffer body = new StringBuffer("CREATE OR REPLACE FUNCTION ");
    body.append("im_"+shortName(table)+"_"+shortName(superTable)+"_UPD() RETURNS TRIGGER AS $BODY$\n");
    body.append("BEGIN\n");
    body.append("  UPDATE "+superTable+"\n  SET");
    boolean needsComma = false;
    for(String field: fields) {
      String dbField = getDBName(field);
      if (needsComma) body.append(",");
      needsComma = true;
      body.append("\n  ");
      body.append("    "+dbField+" = NEW."+dbField);
    }
    body.append("\n  WHERE "+superTable+".id = NEW.id;\n");
    body.append("  RETURN NEW;\nEND;\n");
    body.append("$BODY$ LANGUAGE plpgsql;\n");
    return body.toString();
  }
  private static String makeInsertBody(String table, String superTable, Set<String> fields) {
    StringBuffer body = new StringBuffer( "CREATE OR REPLACE FUNCTION ");
    body.append("im_"+shortName(table)+"_"+shortName(superTable)+"_INS() RETURNS TRIGGER AS $BODY$\n");
    body.append("BEGIN\n");
    body.append("  INSERT INTO "+superTable+" (");
    StringBuffer fieldString = new StringBuffer();
    boolean needsComma = false;
    for(String field: fields) {
      String dbField = getDBName(field);
      if (needsComma) fieldString.append(",");
      needsComma = true;
      fieldString.append(dbField);
    }
    body.append(fieldString+") SELECT "+fieldString+" FROM "+table+"\n");
    body.append("  WHERE id=NEW.id;\n");
    body.append("  RETURN NEW;\nEND;\n");
    body.append("$BODY$ LANGUAGE plpgsql;\n");
    return body.toString();
  } 
  private static String makeDeleteBody(String table, String superTable, Set<String> fields) {
    StringBuffer body = new StringBuffer( "CREATE OR REPLACE FUNCTION ");
    body.append("im_"+shortName(table)+"_"+shortName(superTable)+"_DEL() RETURNS TRIGGER AS $BODY$\n");
    body.append(" BEGIN\n");
    body.append("  DELETE FROM "+superTable+"\n");
    body.append("  WHERE "+superTable+".id = OLD.id;\n");
    body.append("  RETURN OLD;\nEND;\n");
    body.append("$BODY$ LANGUAGE plpgsql;\n");
    return body.toString();
  } 

  private static String writeDefaultClassConstraint(ClassDescriptor cd) {
    String tableName = getDBName(cd.getUnqualifiedName());
    return "ALTER TABLE "+tableName+" ALTER COLUMN class SET NOT NULL;\n"+
    "ALTER TABLE "+tableName+" ALTER COLUMN class SET DEFAULT '"+cd.getName()+"';\n"+
    "ALTER TABLE "+tableName+" ALTER COLUMN id SET DEFAULT nextval('im_serial');\n";
  }

  private static String removeDefaultClassConstraint(ClassDescriptor cd) {
    String tableName = getDBName(cd.getUnqualifiedName());
    return "ALTER TABLE "+tableName+" ALTER COLUMN class DROP NOT NULL;\n"+
    "ALTER TABLE "+tableName+" ALTER COLUMN class DROP DEFAULT;\n"+
    "ALTER TABLE "+tableName+" ALTER COLUMN id DROP DEFAULT;\n";
  }

  private static String writeDisclaimer() {
    /* 
     * This function raises a notice
     */
    return "DO $BODY$\n" +
    "BEGIN\n" +
    "  RAISE NOTICE "+
    "'\nTriggers and stored procedures are being used to propagate your operations\n" +
    "Be sure to backup your database prior to any operations and to remove\n" +
    "all triggers and stored procedures prior to standard InterMine processing.\n" +
    "Using triggers with InterMine is not supported. Use at your own risk!';\n" +
    "END;\n" +
    "  $BODY$ LANGUAGE plpgsql;\n";
  }
  private static String removeDisclaimer() {
    return "DO $BODY$\n" +
        "BEGIN\n" +
        "  RAISE NOTICE "+
        "'\nTriggers and stored procedures should have been removed.\n" +
        "Be sure to confirm all operations were successful by running the\n" +
        "commands \\df and select * from pg_trigger;\n" +
        "Using triggers with InterMine is not supported. Use at your own risk!';\n" +
        "END;\n" +
        "  $BODY$ LANGUAGE plpgsql;\n";
  }

  private static String writeSequence() {
    /*
     * create a sequence for newly inserted objects. If there are any.
     */
    return "CREATE SEQUENCE im_serial;\n"+
    "SELECT setval('im_serial',(select max(id) from intermineobject));\n";
  }
  private static String removeSequence() {
    /*
     * create a sequence for newly inserted objects. We may need to
     * increment the 'main' serial counter if we've added many things
     */
    return "DROP SEQUENCE im_serial;\n"+
     "SELECT setval('serial',(select (max(id)-1)/1000000 from intermineobject));\n";
  }

}