package org.intermine.web.struts;

import java.util.HashMap;

import org.intermine.api.profile.Profile;
import org.intermine.web.logic.Constants;

import servletunit.struts.MockStrutsTestCase;

public class WebappTestCase extends MockStrutsTestCase
{
    
    public WebappTestCase(String name) {
        super(name);
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        //necessary to work-round struts test case not invoking our SessionListener
        getSession().setAttribute(Constants.PROFILE,
            new Profile(null, null, null, null,
                        new HashMap(), new HashMap(), new HashMap(), true, false));
    }

}
