package org.intermine.task;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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

/*
 * Code for generating SQL files for manual CRUD operations on InterMine
 * database.
 *
 * Please see
 * intermine.readthedocs.io/en/latest/database/database-
 * building/post-build-updating-with-sql-triggers for more details.
 *
 * This is an experimental feature. Use at your own risk.
 */

/**
 * @author Joe Carlson <JWCarlson@lbl.gov>
 * @version 0.1
 */
public class GenerateUpdateTriggersTask extends Task
{
    /**
     * If a table name is very long, the names of the triggers and functions may
     * exceed the PostgreSQL limits. This sets the length of the "prefix" that we
     * will use. If there are conflicts, we'll append letters to distinguish them
     * from one another
     */
    private static final int ABBREVIATED_TABLE_NAME_LENGTH = 20;

    /**
     * If the auto-generated name for the triggers and stored procedures gets too
     * long, we may need to abbreviate them. This keeps track of the shortened
     * name.
     */
    private static HashMap<String, String> abbreviations = new HashMap<String, String>();

    /**
     * Where we write the SQL file.
     */
    private File destDir = null;

    /**
     * Sets the destination directory for the SQL files.
     *
     * @param theDestDir
     *          the destination directory
     */
    public final void setDestDir(final File theDestDir) {
        this.destDir = theDestDir;
    }

    /**
     * The name of the ObjectStore.
     */
    private String osname;

    /**
     * Sets the os alias.
     *
     * @param theOsname
     *          the os alias
     */
    public final void setOsName(final String theOsname) {
        this.osname = theOsname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execute() {
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
                throw new RuntimeException(
                        "Cannot connect to objectstore: " + e.getMessage());
            }

            Model model = os.getModel();
            String adderFileName = "add-update-triggers.sql";
            String removerFileName = "remove-update-triggers.sql";

            FileWriter adderW;
            FileWriter removerW;
            try {
                adderW = new FileWriter(new File(destDir, adderFileName));
                removerW = new FileWriter(new File(destDir, removerFileName));
            } catch (IOException e) {
                throw new BuildException("Cannot open SQL file: " + e.getMessage());
            }

            PrintWriter adderPW = new PrintWriter(adderW);
            PrintWriter removerPW = new PrintWriter(removerW);

            adderPW.print(getAddDisclaimer());
            removerPW.print(getRemoveDisclaimer());

            adderPW.print(getAddSequence());
            for (ClassDescriptor cld : model.getBottomUpLevelTraversal()) {

                if (!"InterMineObject".equals(cld.getUnqualifiedName())
                        && cld.getFieldDescriptorByName("id") != null) {

                    adderPW.print(getAddDefaultClassConstraint(cld));
                    removerPW.print(getRemoveDefaultClassConstraint(cld));
                    Set<ClassDescriptor> sCDs = cld.getSuperDescriptors();

                    for (ClassDescriptor superCld : sCDs) {
                        if (!"InterMineObject".equals(superCld.getUnqualifiedName()) ) {
                            adderPW.print(getAddSuperClassActions(cld, superCld));
                            removerPW.print(getRemoveSuperClassActions(cld, superCld));
                        }
                    }

                    adderPW.print(getAddIMOActions(cld));
                    removerPW.print(getRemoveIMOActions(cld));
                }
            }

            removerPW.print(getRemoveSequence());
            adderPW.print(getAddDisclaimer());
            adderPW.close();
            removerPW.print(getRemoveDisclaimer());
            removerPW.close();
        } catch (Exception e) {
            throw new BuildException("Failed to build SQL triggers: " + e.getMessage());
        }
    }

    /**
     * Generate SQL that propagates actions from a table to InterMineObject.
     *
     * @param c
     *          ClassDescriptor for the base table
     * @return SQL to generate functions and triggers
     */
    private static String getAddIMOActions(final ClassDescriptor c) {
        String tn = getDBName(c.getUnqualifiedName());
        String cmds
            = "DROP TRIGGER IF EXISTS "
            + getIMOUpdateTriggerName(tn) + " ON " + tn + ";\n"
            + getIMOUpdateBody(c)
            + "CREATE TRIGGER " + getUpdateTriggerName(tn, "InterMineObject")
            + " AFTER UPDATE ON " + tn + " FOR EACH ROW EXECUTE PROCEDURE "
            + getIMOUpdateFunctionName(tn) + ";\n\n"

            + "DROP TRIGGER IF EXISTS "
            + getIMOInsertTriggerName(tn) + " ON " + tn + ";\n"
            + getIMOInsertBody(c)
            + "CREATE TRIGGER " + getIMOInsertTriggerName(tn)
            + " AFTER INSERT ON " + tn + " FOR EACH ROW EXECUTE PROCEDURE "
            + getIMOInsertFunctionName(tn) + ";\n\n"

            + "DROP TRIGGER IF EXISTS "
            + getIMODeleteTriggerName(tn) + " ON " + tn + ";\n"
            + getIMODeleteBody(c)
            + "CREATE TRIGGER " + getIMODeleteTriggerName(tn)
            + " AFTER DELETE ON " + tn + " FOR EACH ROW EXECUTE PROCEDURE "
            + getIMODeleteFunctionName(tn) + ";\n\n";

        return cmds;
    }

    /**
     * Generate the SQL function that propagates update action to the
     * InterMineObject table.
     *
     * @param c
     *          ClassDescriptor for the table
     * @return SQL to define the function
     */
    private static String getIMOUpdateBody(final ClassDescriptor c) {
        String tn = getDBName(c.getUnqualifiedName());
        StringBuffer body = new StringBuffer(
                "CREATE OR REPLACE FUNCTION " + getIMOUpdateFunctionName(tn)
                + " RETURNS TRIGGER AS $$\n");
        body.append("DECLARE objectText TEXT;\n");
        body.append("BEGIN\n");
        body.append("IF ( NEW.class != '" + c.getName()
                + "' ) THEN RETURN NULL; END IF;\n");
        Set<FieldDescriptor> classFD = c.getAllFieldDescriptors();
        body.append("objectText = '$_^" + c.getName() + "';\n");
        for (FieldDescriptor fD : classFD) {
            if (fD.isAttribute() && !"id".equals(fD.getName()) ) {
                body.append("IF ( NEW." + getDBName(fD.getName())
                        + " IS NOT NULL ) THEN objectText := objectText || " + "'$_^a"
                        + fD.getName() + "$_^'||NEW." + getDBName(fD.getName())
                        + "; END IF;\n");
            } else if (fD.isReference()) {
                body.append("IF ( NEW." + fD.getName()
                        + "id IS NOT NULL ) THEN objectText := objectText || " + "'$_^r"
                        + fD.getName() + "$_^'||NEW." + fD.getName() + "id; END IF;\n");
            }
        }
        body.append("objectText:=objectText||'$_^aid$_^'||NEW.id;\n");
        body.append("EXECUTE 'UPDATE intermineobject SET object=$1 WHERE id=$2 and "
                + "class=$3' USING objectText,NEW.id,'" + c.getName() + "';\n");
        body.append("RETURN NEW;\nEND;\n $$ LANGUAGE plpgsql;\n\n");

        return body.toString();
    }

    /**
     * Generate the SQL function that propagates insert action to the
     * InterMineObject table.
     *
     * @param c
     *          ClassDescriptor for the table
     * @return SQL to define the function
     */
    private static String getIMOInsertBody(final ClassDescriptor c) {
        String tn = getDBName(c.getUnqualifiedName());
        StringBuffer body = new StringBuffer(
            "CREATE OR REPLACE FUNCTION " + getIMOInsertFunctionName(tn)
            + " RETURNS TRIGGER AS $$\n");
        body.append("DECLARE objectText TEXT;\n");
        body.append("DECLARE objectid INT;\n");
        body.append("BEGIN\n");
        body.append("IF ( NEW.class != '" + c.getName()
                + "' ) THEN RETURN NULL; END IF;\n");
        Set<FieldDescriptor> classFD = c.getAllFieldDescriptors();
        body.append("objectText = '$_^" + c.getName() + "';\n");
        for (FieldDescriptor fD : classFD) {
            if (fD.isAttribute() && !"id".equals(fD.getName()) ) {
                body.append("IF ( NEW." + getDBName(fD.getName())
                        + " IS NOT NULL ) THEN objectText := objectText || " + "'$_^a"
                        + fD.getName() + "$_^'||NEW." + getDBName(fD.getName())
                        + "; END IF;\n");
            } else if (fD.isReference()) {
                body.append("IF ( NEW." + fD.getName()
                        + "id IS NOT NULL ) THEN objectText := objectText || " + "'$_^r"
                        + fD.getName() + "$_^'||NEW." + fD.getName() + "id; END IF;\n");
            }
        }
        body.append("objectText:=objectText||'$_^aid$_^'||NEW.id;\n");
        body.append("objectId := NEW.id;\n");
        body.append("EXECUTE 'INSERT INTO intermineobject (id,class,object) values "
                + "($1,$2,$3)' USING objectId,'" + c.getName() + "',objectText;\n");
        body.append("RETURN NEW;\nEND;\n $$ LANGUAGE plpgsql;\n\n");

        return body.toString();
    }

    /**
     * Generate the SQL function that propagates delete action to the
     * InterMineObject table.
     *
     * @param c
     *          ClassDescriptor for the table
     * @return SQL to define the function
     */
    private static String getIMODeleteBody(final ClassDescriptor c) {
        String tn = getDBName(c.getUnqualifiedName());
        StringBuffer body = new StringBuffer(
            "CREATE OR REPLACE FUNCTION " + getIMODeleteFunctionName(tn)
            + " RETURNS TRIGGER AS $$\n");
        body.append("BEGIN\n");
        body.append("IF ( OLD.class != '" + c.getName()
                + "' ) THEN RETURN NULL; END IF;\n");
        body.append("DELETE FROM intermineobject WHERE id=OLD.id;\n");
        body.append("RETURN OLD;\n");
        body.append("END;\n $$ LANGUAGE plpgsql;\n\n");

        return body.toString();
    }

    /**
     * Generate the SQL function that removes triggers and functions that
     * propagate actions from a table to the InterMineObject table.
     *
     * @param c
     *          ClassDescriptor for the table
     * @return SQL to define the function
     */
    private static String getRemoveIMOActions(final ClassDescriptor c) {
        String tn = getDBName(c.getUnqualifiedName());
        String cmds =
            "DROP TRIGGER IF EXISTS " + getIMOUpdateTriggerName(tn)
            + " ON " + tn + ";\n"
            + "DROP TRIGGER IF EXISTS " + getIMOInsertTriggerName(tn)
            + " ON " + tn + ";\n"
            + "DROP TRIGGER IF EXISTS " + getIMODeleteTriggerName(tn)
            + " ON " + tn + ";\n"
            + "DROP FUNCTION IF EXISTS " + getIMOInsertFunctionName(tn) + ";\n"
            + "DROP FUNCTION IF EXISTS " + getIMOUpdateFunctionName(tn) + ";\n"
            + "DROP FUNCTION IF EXISTS " + getIMODeleteFunctionName(tn) + ";\n\n";

        return cmds;
    }

    /**
     * Generate the SQL to create the triggers that propagates actions from a
     * table to its super table.
     *
     * @param c
     *          ClassDescriptor for the base table
     * @param s
     *          ClassDescriptor for the super table
     * @return SQL to generate functions and triggers
     */
    private static String getAddSuperClassActions(final ClassDescriptor c,
            final ClassDescriptor s) {
        String tn = getDBName(c.getUnqualifiedName());
        String stn = getDBName(s.getUnqualifiedName());

        Set<FieldDescriptor> superFD = s.getAllFieldDescriptors();
        Set<FieldDescriptor> classFD = c.getAllFieldDescriptors();
        HashSet<String> commonFields = new HashSet<String>();
        commonFields.add("class");
        for (FieldDescriptor fD : superFD) {
            if (classFD.contains(fD)) {
                if (fD.isAttribute()) {
                    commonFields.add(fD.getName());
                } else if (fD.isReference()) {
                    commonFields.add(fD.getName() + "id");
                }
            }
        }

        String cmds =
            "DROP TRIGGER IF EXISTS " + getUpdateTriggerName(tn, stn)
            + " ON " + tn + ";\n"
            + getUpdateBody(tn, stn, commonFields)
            + "CREATE TRIGGER " + getUpdateTriggerName(tn, stn)
            + " AFTER UPDATE ON " + tn
            + " FOR EACH ROW EXECUTE PROCEDURE "
            + getUpdateFunctionName(tn, stn) + ";\n"

            + "DROP TRIGGER IF EXISTS " + getInsertTriggerName(tn, stn)
            + " ON " + tn + ";\n"
            + getInsertBody(tn, stn, commonFields)
            + "CREATE TRIGGER " + getInsertTriggerName(tn, stn)
            + " AFTER INSERT ON " + tn
            + " FOR EACH ROW EXECUTE PROCEDURE "
            + getInsertFunctionName(tn, stn) + ";\n"

            + "DROP TRIGGER IF EXISTS " + getDeleteTriggerName(tn, stn)
            + " ON " + tn + ";\n"
            + getDeleteBody(tn, stn)
            + "CREATE TRIGGER " + getDeleteTriggerName(tn, stn)
            + " AFTER DELETE ON " + tn
            + " FOR EACH ROW EXECUTE PROCEDURE "
            + getDeleteFunctionName(tn, stn) + ";\n";

        return cmds;
    }

    private static String getIMOInsertTriggerName(String tn) {
        return getInsertTriggerName(tn, "InterMineObject");
    }

    private static String getIMOUpdateTriggerName(String tn) {
        return getUpdateTriggerName(tn, "InterMineObject");
    }

    private static String getIMODeleteTriggerName(String tn) {
        return getDeleteTriggerName(tn, "InterMineObject");
    }

    private static String getInsertTriggerName(String tn, String stn) {
        return getTriggerName(tn, stn, "INS");
    }

    private static String getUpdateTriggerName(String tn, String stn) {
        return getTriggerName(tn, stn, "UPD");
    }

    private static String getDeleteTriggerName(String tn, String stn) {
        return getTriggerName(tn, stn, "DEL");
    }

    private static String getTriggerName(String tn, String stn, String type) {
        return String.format(
            "im_%s_%s_%s_tg",
            shortName(tn), shortName(stn), type);
    }

    private static String getIMOInsertFunctionName(String tn) {
        return getInsertFunctionName(tn, "InterMineObject");
    }

    private static String getIMOUpdateFunctionName(String tn) {
        return getUpdateFunctionName(tn, "InterMineObject");
    }

    private static String getIMODeleteFunctionName(String tn) {
        return getDeleteFunctionName(tn, "InterMineObject");
    }

    private static String getInsertFunctionName(String tn, String stn) {
        return getFunctionName(tn, stn, "INS");
    }

    private static String getUpdateFunctionName(String tn, String stn) {
        return getFunctionName(tn, stn, "UPD");
    }

    private static String getDeleteFunctionName(String tn, String stn) {
        return getFunctionName(tn, stn, "DEL");
    }

    private static String getFunctionName(String tn, String stn, String type) {
        return String.format(
            "im_%s_%s_%s()", shortName(tn), shortName(stn), type);
    }

    /**
     * Write the SQL function that propagates update action to the super table.
     *
     * @param tn
     *          base table where the update action originates
     * @param stn
     *          super table that the update action propagates to
     * @param fields
     *          fields in the table which are common to the super table
     * @return SQL to define the function
     */
    private static String getUpdateBody(final String tn,
            final String stn, final Set<String> fields) {
        StringBuffer body = new StringBuffer("CREATE OR REPLACE FUNCTION ");
        body.append(
            getUpdateFunctionName(tn, stn) + " RETURNS TRIGGER AS $BODY$\n");
        body.append("BEGIN\n");
        body.append("  UPDATE " + stn + "\n  SET");
        boolean needsComma = false;
        for (String field : fields) {
            String dbField = getDBName(field);
            if (needsComma) {
                body.append(",");
            }
            needsComma = true;
            body.append("\n  ");
            body.append("    " + dbField + " = NEW." + dbField);
        }
        body.append("\n  WHERE " + stn + ".id = NEW.id;\n");
        body.append("  RETURN NEW;\nEND;\n");
        body.append("$BODY$ LANGUAGE plpgsql;\n");
        return body.toString();
    }

    /**
     * Generate the SQL function that propagates insert action to the super table.
     *
     * @param tn
     *          base table where the insert action originates
     * @param stn
     *          super table that the insert action propagates to
     * @param fields
     *          fields in the table which are common to the super table
     * @return SQL to define the function
     */
    private static String getInsertBody(final String tn,
            final String stn, final Set<String> fields) {
        StringBuffer body = new StringBuffer("CREATE OR REPLACE FUNCTION ");
        body.append(
            getInsertFunctionName(tn, stn) + " RETURNS TRIGGER AS $BODY$\n");
        body.append("BEGIN\n");
        body.append("  INSERT INTO " + stn + " (");
        StringBuffer fieldString = new StringBuffer();
        boolean needsComma = false;
        for (String field : fields) {
            String dbField = getDBName(field);
            if (needsComma) {
                fieldString.append(",");
            }
            needsComma = true;
            fieldString.append(dbField);
        }
        body.append(
                fieldString + ") SELECT " + fieldString + " FROM " + tn + "\n");
        body.append("  WHERE id=NEW.id;\n");
        body.append("  RETURN NEW;\nEND;\n");
        body.append("$BODY$ LANGUAGE plpgsql;\n");
        return body.toString();
    }

    /**
     * Generate the SQL function that propagates delete action to the super table.
     *
     * @param tn
     *          base table where the delete action originates
     * @param stn
     *          super table that the delete action propagates to
     * @return SQL to define the function
     */
    private static String getDeleteBody(final String tn,
            final String stn) {
        StringBuffer body = new StringBuffer("CREATE OR REPLACE FUNCTION ");
        body.append(
            getDeleteFunctionName(tn, stn) + " RETURNS TRIGGER AS $BODY$\n");
        body.append(" BEGIN\n");
        body.append("  DELETE FROM " + stn + "\n");
        body.append("  WHERE " + stn + ".id = OLD.id;\n");
        body.append("  RETURN OLD;\nEND;\n");
        body.append("$BODY$ LANGUAGE plpgsql;\n");

        return body.toString();
    }

    /**
     * Generate the SQL to remove functions and triggers that propagate actions
     * from a table to its super table.
     *
     * @param c
     *          ClassDescriptor for the base table
     * @param s
     *          ClassDescriptor for the super table
     * @return SQL to remove functions and triggers
     */
    private static String getRemoveSuperClassActions(final ClassDescriptor c,
            final ClassDescriptor s) {
        String tn = getDBName(c.getUnqualifiedName());
        String stn = getDBName(s.getUnqualifiedName());

        String cmds = "DROP TRIGGER IF EXISTS " + getInsertTriggerName(tn, stn)
            + " ON " + tn + ";\n"
            + "DROP TRIGGER IF EXISTS " + getUpdateTriggerName(tn, stn)
            + " ON " + tn + ";\n"
            + "DROP TRIGGER IF EXISTS " + getDeleteTriggerName(tn, stn)
            + " ON " + tn + ";\n"
            + "DROP FUNCTION IF EXISTS " + getInsertFunctionName(tn, stn) + ";\n"
            + "DROP FUNCTION IF EXISTS " + getUpdateFunctionName(tn, stn) + ";\n"
            + "DROP FUNCTION IF EXISTS " + getDeleteFunctionName(tn, stn) + ";\n\n";

        return cmds;
    }

    /**
     * Generate the SQL for adding a default constraint on the class column.
     *
     * @param cd
     *          The ClassDescriptor for the affected table
     * @return SQL for adding the table constraint.
     */
    private static String getAddDefaultClassConstraint(final ClassDescriptor cd) {
        String tableName = getDBName(cd.getUnqualifiedName());
        return "ALTER TABLE " + tableName + " ALTER COLUMN class SET NOT NULL;\n"
            + "ALTER TABLE " + tableName + " ALTER COLUMN class SET DEFAULT '"
            + cd.getName() + "';\n" + "ALTER TABLE " + tableName
            + " ALTER COLUMN id SET DEFAULT nextval('im_post_build_insert_serial');\n";
    }

    /**
     * Generate the SQL for removing a default constraint on the class column.
     *
     * @param cd
     *          The ClassDescriptor for the affected table
     * @return SQL for removing the table constraint.
     */
    private static String getRemoveDefaultClassConstraint(final ClassDescriptor cd) {
        String tableName = getDBName(cd.getUnqualifiedName());
        return "ALTER TABLE " + tableName + " ALTER COLUMN class DROP NOT NULL;\n"
            + "ALTER TABLE " + tableName + " ALTER COLUMN class DROP DEFAULT;\n"
            + "ALTER TABLE " + tableName + " ALTER COLUMN id DROP DEFAULT;\n";
    }

    /**
     * Write a notice that will appear when the triggers are created.
     *
     * @return SQL for writing the notice.
     */
    private static String getAddDisclaimer() {
        /*
         * Generate SQL that write a notice to appear when the triggers are added.
         *
         * @return SQL for writing the notice.
         */
        return "DO $BODY$\n" + "BEGIN\n" + "  RAISE NOTICE "
            + "'\nTriggers and stored procedures are used to propagate operations\n"
            + "Backup your database prior to any operations and to remove\n"
            + "triggers and stored procedures prior to InterMine processing.\n"
            + "Using triggers with InterMine is not supported!\n"
            + "Use at your own risk!';\n" + "END;\n"
            + "  $BODY$ LANGUAGE plpgsql;\n";
    }

    /**
     * Generate SQL that write a notice to appear when the triggers are removed.
     *
     * @return SQL for writing the notice.
     */
    private static String getRemoveDisclaimer() {
        return "DO $BODY$\n" + "BEGIN\n" + "  RAISE NOTICE "
                + "'\nTriggers and stored procedures should have been removed.\n"
                + "Be sure to confirm all operations were successful by running the\n"
                + "commands \\df and select * from pg_trigger;\n"
                + "Using triggers with InterMine is not supported. "
                + "Use at your own risk!';\n" + "END;\n"
                + "  $BODY$ LANGUAGE plpgsql;\n";
    }

    /**
     * Get a db-safe version of a word.
     *
     * Attribute names may need some rewriting to become good column names.
     *
     * The one exception is 'Class'. It is considered to be an SQL reserved word,
     * but we use it as a column name
     *
     * @param word
     *          token to rewrite if needed
     * @return db-safe version.
     */
    static String getDBName(final String word) {
        if ("class".equalsIgnoreCase(word) ) {
            return word;
        }
        return DatabaseUtil.generateSqlCompatibleName(word);
    }

    /**
     * Get a db-safe name of a table
     *
     * since we can run into trouble if the length of the function or trigger name
     * exceeds a certain length, let's truncate when we have to. The trigger name
     * can be 63 characters ("show max_identifier_length;") We'll abbreviate
     * things longer than 20 and append a new letter if we have to. That gives
     * some room.
     *
     * This algorithm is pretty crude. But I'm hoping it's "good enough"
     *
     * @param table
     *          table name to rewrite if needed
     * @return db-safe version.
     */
    static String shortName(final String table) {
        if (table.length() < ABBREVIATED_TABLE_NAME_LENGTH) {
            return table;
        }
        // we're more than 20 characters. Take the first 19 and start appending
        // a new letter.
        StringBuffer trialWord = new StringBuffer(
                table.substring(0, ABBREVIATED_TABLE_NAME_LENGTH - 1));
        trialWord.append("A");
        while (abbreviations.containsKey(trialWord)
                && !abbreviations.get(trialWord).equals(table)) {
            char newChar = trialWord.charAt(ABBREVIATED_TABLE_NAME_LENGTH);
            if (newChar == 'Z') {
                throw new BuildException(
                        "There are too many tables starting with the first 19 characters!");
            }
            newChar++;
            trialWord.setCharAt(ABBREVIATED_TABLE_NAME_LENGTH, newChar);
        }
        abbreviations.put(trialWord.toString(), table);
        return trialWord.toString();

    }

    /**
     * Generate SQL to create a sequence to assign id's for new objects.
     *
     * This is used for INSERT statements without id's
     *
     * @return SQL to create the sequence used in inserts.
     */
    private static String getAddSequence() {
        /*
         * create a sequence for newly inserted objects. If there are any.
         */
        return "CREATE SEQUENCE im_post_build_insert_serial;\n"
            + "SELECT setval('im_post_build_insert_serial',(select max(id) from intermineobject));"
            + "\n";
    }

    /**
     * Generate SQL to remove a sequence to assign id's for new objects.
     *
     * @return SQL to drop sequence used in inserts.
     */
    private static String getRemoveSequence() {
        /*
         * create a sequence for newly inserted objects. We may need to increment
         * the 'main' serial counter if we've added many things
         */
        return "DROP SEQUENCE im_post_build_insert_serial;\n"
            + "SELECT setval('serial',(select (max(id)-1)/1000000 "
            + "from intermineobject));\n";
    }
}
