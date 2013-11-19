package org.intermine.api.idresolution;

import java.util.Collection;

public interface JobInput {

    public Collection<String> getIds();

    public String getExtraValue();

    public String getType();

    public Boolean getCaseSensitive();

    public Boolean getWildCards();

}