package org.intermine.api.bag;

import java.util.ArrayList;
import java.util.List;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.TemplateQuery;
import org.intermine.model.userprofile.Tag;

public class TypeConverterHelper
{

    /**
     * Find template queries that are tagged for use as converters.
     * @param superProfile the superuser profile to fetch tags and templates
     * @return a list of conversion templates
     */
    public static List<TemplateQuery> getConversionTemplates(Profile superProfile) {

        List<TemplateQuery> conversionTemplates = new ArrayList<TemplateQuery>();
        TagManager tagManager =
            new TagManagerFactory(superProfile.getProfileManager()).getTagManager();

        List<Tag> tags = tagManager.getTags(TagNames.IM_CONVERTER, null, TagTypes.TEMPLATE,
                superProfile.getUsername());

        for (Tag tag : tags) {
            String oid = tag.getObjectIdentifier();
            TemplateQuery tq = superProfile.getSavedTemplates().get(oid);
            if (tq != null) {
                conversionTemplates.add(tq);
            }
        }
        return conversionTemplates;
    }
}
