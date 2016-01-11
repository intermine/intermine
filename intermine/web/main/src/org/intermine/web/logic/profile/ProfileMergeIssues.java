package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The problems that arose when merging two profiles.
 * @author Alex Kalderimis
 *
 */
public class ProfileMergeIssues
{
    private Map<String, String> renamedBags = new HashMap<String, String>();
    private Map<String, String> renamedTemplates = new HashMap<String, String>();

    /**
     * Record that a template failed.
     * @param oldName the old (invalid) name
     * @param newName the new (valid) name.
     */
    void addFailedTemplate(String oldName, String newName) {
        renamedTemplates.put(oldName, newName);
    }

    /**
     * Get the names of all the templates that failed.
     * @return The names of the failed templates (unmodifiable, never null, might be empty).
     */
    public Map<String, String> getRenamedTemplates() {
        return Collections.unmodifiableMap(renamedTemplates);
    }

    /**
     * Record that a bag was renamed.
     * @param oldName The old name for this bag.
     * @param newName The name it now has.
     */
    void addRenamedBag(String oldName, String newName) {
        renamedBags.put(oldName, newName);
    }

    /**
     * Get the mapping from old to new names.
     * @return The mapping (unmodifiable, never null, might be empty).
     */
    public Map<String, String> getRenamedBags() {
        return Collections.unmodifiableMap(renamedBags);
    }

    /**
     * Combine this set of merge issues with another set.
     * @param that The other merge issues
     * @return A combined set of merge issues.
     */
    public ProfileMergeIssues combineWith(ProfileMergeIssues that) {
        ProfileMergeIssues combined = new ProfileMergeIssues();
        combined.renamedTemplates.putAll(this.renamedTemplates);
        combined.renamedTemplates.putAll(that.renamedTemplates);
        combined.renamedBags.putAll(this.renamedBags);
        combined.renamedBags.putAll(that.renamedBags);
        return combined;
    }

    /**
     * @return whether there are any issues.
     */
    public boolean hasIssues() {
        return !(renamedTemplates.isEmpty() && renamedBags.isEmpty());
    }

}
