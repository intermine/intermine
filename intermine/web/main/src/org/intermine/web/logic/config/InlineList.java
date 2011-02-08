package org.intermine.web.logic.config;

import java.util.List;

public class InlineList {

    private String path;
    private Object object;

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public int getSize() {
        return ((List<?>) object).size();
    }

}
