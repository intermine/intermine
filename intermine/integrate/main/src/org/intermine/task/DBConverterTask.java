package org.intermine.task;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.dataconversion.DBConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

import java.lang.reflect.Constructor;

import org.apache.tools.ant.BuildException;

/**
 * Task to call DBConverters.
 * @author Kim Rutherford
 */
public class DBConverterTask extends ConverterTask
{
    private String dbAlias;
    private String clsName;

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

    /**
     * Run the task
     * @throws BuildException if a problem occurs
     */
    @Override
    public void execute() throws BuildException {
        if (clsName == null) {
            throw new BuildException("clsName attribute is not set");
        }
        if (getOsName() == null) {
            throw new BuildException("osName attribute is not set");
        }
        if (getModelName() == null) {
            throw new BuildException("modelName attribute is not set");
        }
        if (dbAlias == null) {
            throw new BuildException("dbAlias attribute is not set");
        }
        try {
            ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter(getOsName());
            ObjectStoreItemWriter writer = new ObjectStoreItemWriter(osw);
            Database database = DatabaseFactory.getDatabase(dbAlias);

            Class<?> c = Class.forName(clsName);
            if (!DBConverter.class.isAssignableFrom(c)) {
                throw new IllegalArgumentException("Class (" + clsName + ") is not a subclass of "
                                             + DBConverter.class.getName());
            }

            Constructor<?> m = c.getConstructor(new Class[] {Database.class, Model.class, 
                                                             ItemWriter.class});
            Model model = Model.getInstanceByName(getModelName());
            DBConverter converter = 
                (DBConverter) m.newInstance(new Object[] {database, model, writer});
            converter.process();
            converter.getItemWriter().close();
        } catch (Exception e) {
            throw new BuildException("problem while running converter reading from db: "
                                     + dbAlias, e);
        }
    }
}
