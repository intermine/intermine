package org.intermine.api.results;

import org.intermine.model.FastPathObject;
import org.intermine.pathquery.Path;

public interface ResultCell
{
    Object getField();
    
    boolean isKeyField();
    
    Integer getId();
    
    Path getPath();
    
    FastPathObject getObject();
    
    String getType();
}
