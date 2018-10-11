package org.intermine.metadata;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class ClassDescriptorFactory {

    public static final Set<AttributeDescriptor> NO_ATTRS =
            Collections.unmodifiableSet(new HashSet<AttributeDescriptor>());
    public static final Set<ReferenceDescriptor> NO_REFS =
            Collections.unmodifiableSet(new HashSet<ReferenceDescriptor>());
    public static final Set<CollectionDescriptor> NO_COLLS =
            Collections.unmodifiableSet(new HashSet<CollectionDescriptor>());

    private final String packageName;

    public ClassDescriptorFactory(String packageName) {
        this.packageName = packageName;
    }

    public ClassDescriptorFactory() {
        this.packageName = null;
    }

    private String prefix(String name) {
        if (packageName == null) {
            return name;
        }
        return packageName + "." + name;
    }

    private String prefixAll(String... names) {
        String[] prefixed = new String[names.length];
        for (int i = 0, l = names.length; i < l; i++) {
            prefixed[i] = prefix(names[i]);
        }
        return StringUtils.join(prefixed, " ");
    }

    public String getPackageName() {
        return packageName;
    }

    public ClassDescriptor makeClass(String name) {
        return new ClassDescriptor(prefix(name), null, false, NO_ATTRS, NO_REFS, NO_COLLS);
    }

    public ClassDescriptor makeClass(String name, String... supers) {
        return new ClassDescriptor(prefix(name), prefixAll(supers), false, NO_ATTRS, NO_REFS, NO_COLLS);
    }

    public ClassDescriptor makeInterface(String name) {
        return new ClassDescriptor(prefix(name), null, true, NO_ATTRS, NO_REFS, NO_COLLS);
    }

    public ClassDescriptor makeInterface(String name, String... supers) {
        return new ClassDescriptor(prefix(name), prefixAll(supers), true, NO_ATTRS, NO_REFS, NO_COLLS);
    }
}
