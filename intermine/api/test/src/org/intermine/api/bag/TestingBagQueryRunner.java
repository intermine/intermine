package org.intermine.api.bag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateManager;
import org.intermine.template.TemplateQuery;

/**
 * A BagQueryRunner used for testing, for convenience this can use a defined list of conversion
 * templates instead of fetching from the TemplateManager.
 * @author Richard Smith
 *
 */
public class TestingBagQueryRunner extends BagQueryRunner {
    private List<ApiTemplate> conversionTemplates = new ArrayList<ApiTemplate>();

    public TestingBagQueryRunner(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys,
            BagQueryConfig bagQueryConfig, TemplateManager templateManager) {
        super(os, classKeys, bagQueryConfig, templateManager);
    }

    public void setConversionTemplates(List<ApiTemplate> conversionTemplates) {
        this.conversionTemplates = conversionTemplates;
    }

    @Override
    protected List<ApiTemplate> getConversionTemplates() {
        return this.conversionTemplates;
    }
}
