package org.intermine.web.logic.bag;

import java.util.Collection;

import org.intermine.api.idresolution.JobInput;
import org.intermine.web.struts.BuildBagForm;

public final class WebJobInput implements JobInput {

    private final String type;
    private final Collection<String> idents;
    private final boolean caseSensitive;
    private final String extraValue;

    public WebJobInput(String type, Collection<String> idents, BuildBagForm form) {
        this.type = type;
        this.idents = idents;
        this.caseSensitive = form.getCaseSensitive();
        this.extraValue = form.getExtraFieldValue();
    }
    @Override
    public Collection<String> getIds() {
       return idents;
    }

    @Override
    public String getExtraValue() {
        return this.extraValue;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public Boolean getCaseSensitive() {
        return this.caseSensitive;
    }

    @Override
    public Boolean getWildCards() {
        return false;
    }

}
