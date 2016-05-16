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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ReadWritePerformanceTester;



/**
* Ant task to run a simple read/write performance test on the ObjectStore.
* @author Richard Smith
*
*/
public class ReadWritePerformanceTask extends Task
{
   protected String osAlias;

   /**
    * Set the ObjectStore alias.
    *
    * @param alias the ObjectStore alias
    */
   public void setOsAlias(String osAlias) {
       this.osAlias = osAlias;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void execute() {
       ObjectStore os = null;

       try {
           os = ObjectStoreFactory.getObjectStore(osAlias);
           System.out.println("Starting performance test...");
           ReadWritePerformanceTester.testPerformance(os);

       } catch (Exception e) {
           throw new BuildException(e);
       }
   }
}
