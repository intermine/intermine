/*
 * ForwardParameters.java
 *
 * Created on November 1, 2004, 1:58 PM
 */

package org.intermine.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.struts.action.ActionForward;

/**
 * Utility wrapper class for programatically adding parameters to ActionForward paths.
 * The <code>redirect</code> property of the new ActionForward returned by forward()
 * will be equal to the redirect setting of the original ActionForward.
 *
 * @author tom
 */
public class ForwardParameters
{
    /** Original ActionForward. */
    protected ActionForward af;
    /** Map from parameter name to parameter value. */
    protected Map params = new HashMap();
    
    /**
     * Creates a new instance of ForwardParameters.
     *
     * @param af  the ActionForward to append parameters to
     */
    public ForwardParameters(ActionForward af) {
        this.af = af;
    }
    
    /**
     * Add a parameter to the path.
     *
     * @param name   the name of the parameter
     * @param value  the value of the parameter
     * @return       this ForwardParameters object
     */
    public ForwardParameters addParameter(String name, String value) {
        params.put(name, value);
        return this;
    }
    
    /**
     * Construct the resulting ActionForward.
     * 
     * @return  ActionForward with parameters in path
     */
    public ActionForward forward() {
        String path = "";
        Iterator iter = params.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (path.length() > 0) {
                path += "&";
            }
            path += entry.getKey() + "=" + entry.getValue();
        }
        return new ActionForward(af.getPath() + "?" + path, af.getRedirect());
    }
}
