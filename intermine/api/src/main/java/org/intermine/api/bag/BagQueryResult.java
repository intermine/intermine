package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.intermine.model.InterMineObject;


/**
 * Class to hold the results of querying for a bag of objects.  Makes
 * available the matched objects, results that require some user input
 * (issues) and unresolved input.
 *
 * @author Richard Smith
 */
public class BagQueryResult
{
    /**
     * Key of the Map returned by getIssues() when the query for the input string found more than
     * one object.
     */
    public static final String DUPLICATE = "DUPLICATE";

    /**
     * Key of the Map returned by getIssues() when the input string
     */
    public static final String OTHER = "OTHER";

    /**
     * Key of the Map returned by getIssues() when the object found when querying using input string
     * needed to be translated.
     */
    public static final String TYPE_CONVERTED = "TYPE_CONVERTED";

    /**
     * Key of the Map returned by getIssues() when the object found by querying with a wildcard.
     */
    public static final String WILDCARD = "WILDCARD";

    /**
     * keys of the map in a set
     */
    public static final Set<String> ISSUE_KEYS
        = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                DUPLICATE, OTHER, TYPE_CONVERTED, WILDCARD)));

    private Map<Integer, List> matches = new LinkedHashMap<Integer, List>();

    /**
     * A map from issueType -> Query -> Identifier -> FoundThing[]
     *
     * eg:<pre>
     *     { "DUPLICATE":      { "Q1": { "my-gene-id": [o1, o2] },
     *                           "Q2": { "my-gene-id": [o3, o4] }},
     *       "TYPE_CONVERTED": { "Q3": { "my-prot-id": [pair]   }}}
     * </pre>
     **/
    private Map<String, Map<String, Map<String, List>>> issues =
        new LinkedHashMap<String, Map<String, Map<String, List>>>();

    private final Set<String> unresolved = new LinkedHashSet<String>();

    /**
     * Get any results that require some user input before adding to the bag.
     * [issue type -&gt; [query -&gt; [input string -&gt; List of InterMineObjects]]] or for issue
     * type of "TYPE_TRANSLATED": [issue type -&gt; [query -&gt; [input string -&gt; List of
     * ConvertedObjectPair]]
     * @return a map from issues type to queries to input to possible objects
     */
    public Map<String, Map<String, Map<String, List>>> getIssues() {
        return issues;
    }

    /**
     * Add an issue to this result.
     * @param type one of the type constants from BagQueryResult
     * @param query the name of the query that generated this issue
     * @param input the input identifier
     * @param objects the objects found for the input identifiers
     */
    public void addIssue(String type, String query, String input, List objects) {
        Map<String, Map<String, List>> issuesOfType = issues.get(type);
        if (issuesOfType == null) {
            issuesOfType = new LinkedHashMap<String, Map<String, List>>();
            issues.put(type, issuesOfType);
        }
        Map<String, List> queryIssues = issuesOfType.get(query);
        if (queryIssues == null) {
            queryIssues = new LinkedHashMap<String, List>();
            issuesOfType.put(query, queryIssues);
        }
        List queryObjects = queryIssues.get(input);
        if (queryObjects == null) {
            queryObjects = new ArrayList();
            queryIssues.put(input, queryObjects);
        }
        queryObjects.addAll(objects);
    }

    /**
     * Get any exact matches found by the queries [id -&gt; [input strings].
     * If the same input string appears twice in the initial list it will
     * appear twice in the list of inputs matching the InterMineObject id.
     * @return a map from InterMineObject id to list of input strings
     */
    public Map<Integer, List> getMatches() {
        return matches;
    }

    /**
     * Get ids of all InterMineObjects returned that were matches or issues for this
     * bag query lookup.
     * @return the set of all ids that were matches or issues
     */
    public Set<Integer> getMatchAndIssueIds() {
        Set<Integer> ids = new HashSet<Integer>();
        ids.addAll(matches.keySet());
        ids.addAll(getIssueIds());
        return ids;
    }

    /**
     * Get ids of all InterMineObjects returned that were issues for this
     * bag query lookup.
     * @return the set of all ids that were issues
     */
    public Set<Integer> getIssueIds() {
        Set<Integer> ids = new HashSet<Integer>();
        for (String issueKey: issues.keySet()) {
            ids.addAll(getIssueIds(issueKey));
        }
        return ids;
    }

    /**
     * Get ids of all InterMineObjects returned that were registered as
     * issues of this particular type for this bag query lookup.
     * @param issueKey The type of issue we want (eg "DUPLICATE").
     * @return the set of all ids that were issues
     */
    public Set<Integer> getIssueIds(String issueKey) {
        Set<Integer> ids = new HashSet<Integer>();
        for (IssueResult issue : getIssueResults(issueKey)) {
            // Don't care about the input identifier itself, just the matches.
            for (Object obj : issue.results) {
                if (obj instanceof InterMineObject) {
                    ids.add(((InterMineObject) obj).getId());
                } else if (obj instanceof ConvertedObjectPair) {
                    ids.add(((ConvertedObjectPair) obj).getNewObject().getId());
                } else if (obj instanceof Integer) {
                    ids.add((Integer) obj);
                }
            }
        }
        return ids;
    }

    /**
     * @param issueKey key for issue
     * @return set of issues
     */
    public Set<IssueResult> getIssueResults(String issueKey) {
        Set<IssueResult> result = new HashSet<IssueResult>();
        Map<String, Map<String, List>> issueTypes = issues.get(issueKey);
        if (issueTypes == null) {
            if (ISSUE_KEYS.contains(issueKey)) {
                return result;
            } else {
                throw new IllegalArgumentException(issueKey + " is not a valid issue type");
            }
        }
        for (Entry<String, Map<String, List>> issuesForQuery: issueTypes.entrySet()) {
            String queryDesc = issuesForQuery.getKey();
            for (Entry<String, List> issueSet: issuesForQuery.getValue().entrySet()) {
                result.add(new IssueResult(queryDesc, issueSet.getKey(), issueSet.getValue()));
            }
        }
        return result;
    }

    /**
     * Simple struct to hold three pieces of information together.
     * @author Alex Kalderimis
     */
    public static class IssueResult
    {

        /**
         * query description
         */
        private final String queryDesc;

        /**
         * identifier input
         */
        private final String inputIdent;

        /**
         * matches found
         */
        private final List results;



        /**
         * @param queryDesc query description
         * @param inputIdent identifier input by user
         * @param results matches found
         */
        protected IssueResult(String queryDesc, String inputIdent, List results) {
            this.queryDesc = queryDesc;
            this.inputIdent = inputIdent;
            this.results = Collections.unmodifiableList(results);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        /**
         * @return query description
         */
        public String getQueryDesc() {
            return queryDesc;
        }

        /**
         * @return identifier input by user
         */
        public String getInputIdent() {
            return inputIdent;
        }

        /**
         * @return matches found
         */
        public List getResults() {
            return results;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(queryDesc).append(inputIdent).
                    append(results).hashCode();
        }
    }

    /**
     * @param issueKey key for the issue
     * @return set of identifiers
     */
    public Set<String> getInputIdentifiersForIssue(String issueKey) {
        Set<String> ids = new HashSet<String>();
        for (IssueResult issue : getIssueResults(issueKey)) {
            ids.add(issue.inputIdent);
        }
        return ids;
    }

    /**
     * Add a new match from an input string to an InterMineObject id.
     * @param input the original input string entered
     * @param id the id of an InterMineObject
     */
    public void addMatch(String input, Integer id) {
        List<String> inputs = matches.get(id);
        if (inputs == null) {
            inputs = new ArrayList<String>();
            matches.put(id, inputs);
        }
        inputs.add(input);
    }

    /**
     * Get a Map of any input Strings for which objects of the right type could not be found.
     * @return a Map of from input string to null
     *
     * The values of this map are always null - and this method should be avoided.
     *
     * Changes to the returned map will not affect the information in this bag query result.
     * @deprecated Use getUnresolvedIdentifiers
     */
    @Deprecated
    public Map<String, Object> getUnresolved() {
        Map<String, Object> ret = new HashMap<String, Object>();
        for (String notFound: unresolved) {
            ret.put(notFound, null);
        }
        return ret;
    }

    /**
     * Get all the unresolved identifiers.
     * @return a collection of unresolved identifiers.
     */
    public Collection<String> getUnresolvedIdentifiers() {
        return new HashSet<String>(unresolved);
    }

    /**
     * Set the Map of unresolved input strings.  It is Map from input string to null/object - null
     * when the input doesn't match any object of any type, otherwise a reference to the object
     * that matched.
     *
     * @param unresolvedIdentifiers the unresolved identifiers to add to this result.
     */
    public void addUnresolved(Collection<String> unresolvedIdentifiers) {
        unresolved.addAll(unresolvedIdentifiers);
    }
}
