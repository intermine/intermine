package org.intermine.web.bag;

/* 
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.String;

import org.apache.struts.action.ActionForm;

/**
 * Form for the bagUploadConfirm page.
 * @author Kim Rutherford
 */
public class BagUploadConfirmForm extends ActionForm
{
    private String bagName;
    private String bagType;
    private String matchIDs;
    
    /**
     * Constructor
     */
    public BagUploadConfirmForm() {
        initialise();
    }

    /**
     * Initialise the form.
     */
   public void initialise() {
        bagName = "";
        matchIDs = "";
    }

   /**
    * Set the bag name.
    * @param name the bag name
    */
   public void setBagName(String name) {
       bagName = name;
   }
   
   /**
    * Get the bag name.
    * @return the bag name
    */
   public String getBagName() {
       return bagName;
   }

   
   
   /**
    * Get the encoded match ids (hidden form field).
    * @return the match IDs
    */
   public String getMatchIDs() {
       return matchIDs;
   }

   /**
    * Set the encoded match ids.
    * @param matchIDs the encoded match ids
    */
   public void setMatchIDs(String matchIDs) {
       this.matchIDs = matchIDs;
   }

   /**
    * Get the bag type
    * @return the bag type
    */
   public String getBagType() {
       return bagType;
   }

   /**
    * Set the bag type
    * @param bagType the new bag type
    */
   public void setBagType(String bagType) {
       this.bagType = bagType;
   }
}
