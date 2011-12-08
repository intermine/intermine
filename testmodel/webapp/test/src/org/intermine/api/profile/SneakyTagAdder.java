package org.intermine.api.profile;

/**
 * Class for subverting method access permissions.
 * @author Alex Kalderimis
 *
 */
public class SneakyTagAdder {

    TagManager tm;

    public SneakyTagAdder(TagManager tm) {
        this.tm = tm;
    }

    public void sneakilyAddTag(String tagName, String id, String type, String profile) {
        tm.addTag(tagName, id, type, profile);
    }
}
