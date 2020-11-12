package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.metadata.*;
import org.intermine.sql.DatabaseUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jerven Bolleman
 * @author Daniela Butano
 */
public class R2RMLMappingProcess extends PostProcessor
{
    static final int FORMAT_VERSION = 1;
    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public R2RMLMappingProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     */
    public void postProcess() throws Exception {
        System.out .println("R2RMLMappingProcess...");
        Model model = Model.getInstanceByName("genomic");
        Set<ClassDescriptor> classDescriptors = model.getClassDescriptors();
        Set<CollectionDescriptor> indirections = new HashSet<CollectionDescriptor>();

        for (ClassDescriptor cd :classDescriptors) {
            String table = cd.getSimpleName();

            System.out.println("TABLE: " + DatabaseUtil.getTableName(cd));
            for (FieldDescriptor fd : cd.getAllFieldDescriptors()) {
                String columnName = DatabaseUtil.getColumnName(fd);
                if (fd instanceof AttributeDescriptor) {
                    System.out.println(columnName +
                            (columnName.equalsIgnoreCase("id") ? ": PRIMARY KEY" : ": column")
                            + " with type " + ((AttributeDescriptor) fd).getType());
                } else if (!fd.isCollection()) { //n to one relation
                    System.out.println(columnName + ": FOREIGN KEY with type java.lang.Integer referring to "
                            + ((ReferenceDescriptor) fd).getReferencedClassDescriptor().getSimpleName() + ".id");
                }
            }
            System.out.println();

            //joining table
            for (CollectionDescriptor collection : cd.getCollectionDescriptors()) {
                if (FieldDescriptor.M_N_RELATION == collection.relationType()) {
                    if (!indirections.contains(collection.getReverseReferenceDescriptor())) {
                        indirections.add(collection);
                        String indirectionTable = DatabaseUtil.getIndirectionTableName(collection);
                        System.out.println("JOINING TABLE: " + indirectionTable);
                        String column1 = DatabaseUtil.getInwardIndirectionColumnName(collection, FORMAT_VERSION);
                        String column2 = DatabaseUtil.getOutwardIndirectionColumnName(collection, FORMAT_VERSION);
                        System.out.println(column1 + ": FOREIGN KEY with type java.lang.Integer referring to "
                                + table + ".id");
                        System.out.println(column2 + ": FOREIGN KEY with type java.lang.Integer referring to "
                                + collection.getReferencedClassDescriptor().getName() + (".id"));
                        System.out.println();
                    }
                }
            }
        }
    }
}
