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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * Form for the bagUploadConfirm page.
 * @author Kim Rutherford
 */
public class BagUploadConfirmForm extends ActionForm
{
    private String bagName;
    private String bagType;
    private String matchIDs;
    private String[] selectedObjects;
    
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
        selectedObjects = new String[] {};
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
    * Sets the ids of the selected objects - ie. those that should be included in the new bag
    * @param selectedObjects the selected objects
    */
   public void setSelectedObjects(String[] selectedObjects) {
       this.selectedObjects = selectedObjects;
   }

   /**
    * Gets the ids of the selected objects
    * @return the selected objects
    */
   public String[] getSelectedObjects() {
       return selectedObjects;
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
    * Get the bag type - hidden form value.
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
   
   /**
    * @see ActionForm#validate(ActionMapping, HttpServletRequest)
    */
   public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
       ActionErrors errors = new ActionErrors();
       if (bagName.equals("") && request.getParameter("goBack") == null) {
           errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.savebag.blank"));
       }
       return errors;
   }
}
