package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.text.DateFormat;
import java.text.ParseException;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionError;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.util.TypeUtil;

/**
 * The main form, using for editing constraints
 * @author Mark Woodbridge
 */
public class MainForm extends ActionForm
{
    protected String constraintOp, constraintValue, path, subclass;
    Object parsedConstraintValue;

    /**
     * Gets the value of subclass
     *
     * @return the value of subclass
     */
    public String getSubclass()  {
        return subclass;
    }

    /**
     * Sets the value of subclass
     *
     * @param subclass Value to assign to subclass
     */
    public void setSubclass(String subclass) {
        this.subclass = subclass;
    }
    /**
     * Gets the value of constraintOp
     *
     * @return the value of constraintOp
     */
    public String getConstraintOp()  {
        return constraintOp;
    }

    /**
     * Sets the value of constraintOp
     *
     * @param constraintOp Value to assign to constraintOp
     */
    public void setConstraintOp(String constraintOp) {
        this.constraintOp = constraintOp;
    }

    /**
     * Gets the value of constraintValue
     *
     * @return the value of constraintValue
     */
    public String getConstraintValue()  {
        return constraintValue;
    }

    /**
     * Sets the value of constraintValue
     *
     * @param constraintValue Value to assign to constraintValue
     */
    public void setConstraintValue(String constraintValue) {
        this.constraintValue = constraintValue;
    }

    /**
     * Gets the value of path
     *
     * @return the value of path
     */
    public String getPath()  {
        return path;
    }

    /**
     * Sets the value of path
     *
     * @param path Value to assign to path
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Gets the value of parsedConstraintValue
     *
     * @return the value of parsedConstraintValue
     */
    public Object getParsedConstraintValue() {
        return parsedConstraintValue;
    }

    /**
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        Locale locale = (Locale) session.getAttribute(Globals.LOCALE_KEY);

        ActionErrors errors = new ActionErrors();

        FieldDescriptor fd = MainHelper.getFieldDescriptor(path, model);
        if (fd.isAttribute() && constraintValue != null) {
            AttributeDescriptor attr = (AttributeDescriptor) fd;
            Class fieldClass = TypeUtil.instantiate(attr.getType());
            if (Date.class.equals(fieldClass)) {
                DateFormat df =  DateFormat.getDateInstance(DateFormat.SHORT,
                                                            locale);
                try {
                    parsedConstraintValue = df.parse(constraintValue);
                } catch (ParseException e) {
                    errors.add(ActionErrors.GLOBAL_ERROR,
                               new ActionError("errors.date",
                                               constraintValue,
                                               df.format(new Date())));
                }
            } else {
                try {
                    parsedConstraintValue = TypeUtil.stringToObject(fieldClass, constraintValue);
                } catch (NumberFormatException e) {
                    String shortName = TypeUtil.unqualifiedName(fieldClass.getName()).toLowerCase();
                    errors.add(ActionErrors.GLOBAL_ERROR,
                               new ActionError("errors." + shortName,
                                               constraintValue));
                }
            }
        } else {
            parsedConstraintValue = constraintValue;
        }
        
        if (errors.size() > 0) {
            request.setAttribute("editingNode",
                                 ((Map) session.getAttribute(Constants.QUERY)).get(path));
        }
        
        return errors;
    }
    /**
     * @see ActionForm#reset
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        constraintOp = null;
        constraintValue = null;
        path = null;
        subclass = null;
    }
}
