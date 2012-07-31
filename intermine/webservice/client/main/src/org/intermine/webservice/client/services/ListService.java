package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.collections.TransformerUtils.invokerTransformer;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.Constraints;
import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.MultiPartRequest;
import org.intermine.webservice.client.core.Request;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.lists.ItemList;
import org.intermine.webservice.client.lists.Lists;
import org.intermine.webservice.client.results.Item;
import org.intermine.webservice.client.util.HttpConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An interface to all the resources that represent list information
 * and list operations available at an InterMine implementation.
 *
 * Normally the user should obtain an instance of this class through the
 * ServiceFactory class. Any authentication provided to that parent class
 * will be used in all requests that need authentication by the ListService.
 *
 * The services provided include:
 * <ul>
 *   <li>Getting information about the available lists</li>
 *   <li>Getting information about which lists an item is in</li>
 *   <li>Creating new lists</li>
 *   <li>Modifying lists by adding items to them</li>
 *   <li>Creating new lists from set operations between existing lists</li>
 *   <li>Renaming lists</li>
 *   <li>Deleting lists</li>
 * </ul>
 *
 * @author Alex Kalderimis
 * @version 2
 **/
public class ListService extends Service
{

    private static final String SERVICE_RELATIVE_URL = "listswithobject/json";
    private static final String LISTS_PATH = "/lists/json";
    private static final String QUERY_LISTS_PATH = "/query/tolist/json";
    private static final String LIST_APPEND_PATH = "/lists/append/json";
    private static final String MERGE_PATH = "/lists/union/json";
    private static final String QUERY_APPEND_PATH = "/query/append/tolist/json";
    private static final String INTERSECT_PATH = "/lists/intersect/json";
    private static final String DIFF_PATH = "/lists/diff/json";
    private static final String SUBTRACT_PATH = "/lists/subtract/json";
    private static final String RENAME_PATH = "/lists/rename/json";
    private static final String TAGS = "/list/tags/json";

    /**
     * Use {@link ServiceFactory} instead of constructor for creating this service .
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public ListService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    /** A request implementation for use in this service **/
    private static class ListRequest extends RequestImpl
    {
        public ListRequest(RequestType type, String serviceUrl, ContentType contentType) {
            super(type, serviceUrl, contentType);
        }

        public void setPublicId(String publicId) {
            setParameter("publicId", publicId);
        }

        public void setObjectType(String type) {
            setParameter("type", type);
        }

        public void setDatabaseId(String id) {
            setParameter("id", id);
        }
    }

    /** A multi-part request implementation that is used for creation requests **/
    private static class CreationRequest extends MultiPartRequest
    {
        public CreationRequest(String url) {
            super(url);
        }

        /** Add a file as the contents of the request **/
        public void addContents(File contents) throws FileNotFoundException {
            getParts().add(new FilePart("identifiers", contents.getName(), contents));
        }

        /** Add a string as the contents of the request **/
        public void addContents(String content) {
            getParts().add(new FilePart("identifiers",
                    new ByteArrayPartSource("ids.txt", content.getBytes())));
        }
    }

    /**
     * Returns all accessible InterMine lists containing the object corresponding
     * to the specified publicId.
     *
     * @param publicId An identifier of the object of interest. An object can have different
     * identifiers in different public databases. If there is an object with one of these as
     * a property, the webservice will resolve the given id to that object.
     * @param type the type of Object. Must be a valid name of a class in the service's
     * data-model (eg: "Gene")
     * @throws ServiceException if the identifier does not match an object, or matches
     * more than one.
     * @return A list of ItemList objects that represent lists containing the given object.
     */
    public List<ItemList> getListsWithObject(String publicId, String type) {
        ListRequest request = new ListRequest(RequestType.POST, getUrl(),
                ContentType.APPLICATION_JSON);
        request.setPublicId(publicId);
        request.setObjectType(type);
        return processListsRequest(request);
    }

    /**
     * Returns all accessible InterMine public lists containing the object
     * with the specified DB identifier.
     *
     * @param id The internal DB identifier. This value changes between releases,
     * so this method is only reliable for fetching data when the id has been
     * fetched immediately before the request.
     * @return A list of ItemList objects that represent lists containing the given object.
     */
    public List<ItemList> getListsWithObject(int id) {
        ListRequest request = new ListRequest(RequestType.POST, getUrl(),
                ContentType.APPLICATION_JSON);
        request.setDatabaseId(Integer.toString(id));
        return processListsRequest(request);
    }

    /**
     * Returns a map from list-name to list.
     *
     * This method retrieves all accessible lists and returns them as a map, with each list
     * accessible via its name.
     *
     * {@link #getAccessibleLists()}
     * {@link #getList(String)}
     * @return A map of all accessible lists, with the lists accessible by name.
     */
    public Map<String, ItemList> getListMap() {
        List<ItemList> lists = getAccessibleLists();
        Map<String, ItemList> listMap = new TreeMap<String, ItemList>();
        for (ItemList list: lists) {
            listMap.put(list.getName(), list);
        }
        return listMap;
    }

    /**
     * Returns an object representing a list on the server.
     *
     * {@link #getListMap()}
     * {@link #getAccessibleLists()}
     * @param name the name of the list to retrieve.
     * @return The list, or null if it does not exist.
     */
    public ItemList getList(String name) {
        return getListMap().get(name);
    }

    /**
     * Returns all the lists that this service has access to.
     *
     * Note that the return value of this method will differ depending
     * on the authentication credentials supplied, as each user only
     * has access to their own list, and those that are publicly available.
     *
     * {@link #getListMap()}
     * {@link #getList(String)}
     * @return A list of all accessible lists.
     */
    public List<ItemList> getAccessibleLists() {
        ListRequest request = new ListRequest(RequestType.GET,
                getRootUrl() + LISTS_PATH, ContentType.APPLICATION_JSON);
        return processListsRequest(request);
    }

    /**
     * Create a new list on the server by specifying its content.
     *
     * This method abstracts a couple of different REST calls that create lists
     * based on a specification of content. Content may be given as a set of identifiers
     * to resolve into objects, or as a query to collect as a result set. These parameters,
     * and others are bundled into the ListCreationInfo parameter.
     *
     * @param  info  The bundle of options that defines what content the new list will have.
     * @return A new list.
     */
    public ItemList createList(ListCreationInfo info) {
        if (info.type == null) {
            return processListCreationRequest(getQueryToListRequest(info));
        } else {
            return processListCreationRequest(getIdsToListRequest(info));
        }
    }

    /**
     * Append some objects corresponding to identifiers to the given list, returning a
     * list representing the new state.
     *
     * Use {@link ItemList#append(String...)}
     *
     * @param list The list to append the new items to.
     * @param ids  The ids to append to the list.
     * @return A list object representing the new state.
     */
    public ItemList append(ItemList list, String... ids) {
        CreationRequest request = new CreationRequest(getRootUrl() + LIST_APPEND_PATH);
        request.setParameter("name", list.getName());
        StringBuffer sb = new StringBuffer();
        for (String id: ids) {
            sb.append("\"" + id + "\"\n");
        }
        request.addContents(sb.toString());
        return processListCreationRequest(request);
    }

    /**
     * Append some objects corresponding to identifiers to the given list, returning a
     * list representing the new state.
     *
     * Use {@link ItemList#append(Collection)}
     *
     * @param list The list to append the new items to.
     * @param ids  The identifiers of the objects to append to the list.
     * @return A list object representing the new state.
     */
    public ItemList append(ItemList list, Collection<? extends String> ids) {
        return append(list, ids.toArray(new String[ids.size()]));
    }

    /**
     * Append the objects contained in the result set of a given query
     * to the given list, returning a list representing the new state.
     *
     * The query must have a single output column (that of the id of the objects to add)
     * and that column must be on an object of a compatible type to
     * the list it is being appended to.
     *
     * Note that this is the most efficient method of appending to a list from a
     * query's result set.
     *
     * Use {@link ItemList#append(PathQuery)}
     *
     * @param list  The list to append the new items to.
     * @param query The query to run to find objects for the list.
     * @return A list object representing the new state.
     */
    public ItemList append(ItemList list, PathQuery query) {
        ListRequest request = new ListRequest(RequestType.POST,
                getRootUrl() + QUERY_APPEND_PATH, ContentType.APPLICATION_JSON);
        request.setParameter("listName", list.getName());
        request.setParameter("query", query.toXml());
        return processListCreationRequest(request);
    }

    /**
     * Deletes a list on the server.
     *
     * After being deleted, any query or operation that uses this list will throw an exception.
     * @param delendum The list to delete.
     */
    public void deleteList(ItemList delendum) {
        ListRequest request = new ListRequest(RequestType.DELETE,
                getRootUrl() + LISTS_PATH, ContentType.APPLICATION_JSON);
        request.addParameter("name", delendum.getName());
        HttpConnection con = executeRequest(request);
        String body = con.getResponseBodyAsString();
        ItemList deleted = Lists.parseListCreationInfo(getFactory(), body);
        if (!deleted.getName().equals(delendum.getName())) {
            throw new ServiceException("You asked me to delete " + delendum
                    + ", but the server deleted " + deleted);
        }
    }

    // -- MERGERS

    /**
     * Merge one or more lists into a new list.
     *
     * This operation is the equivalent of the set 'union' operation.
     *
     * @param lists The lists to merge.
     * @return A new list
     */
    public ItemList merge(ItemList... lists) {
        return merge(new ListOperationInfo(), Arrays.asList(lists));
    }

    /**
     * Merge one or more lists into a new list.
     *
     * This operation is the equivalent of the set 'union' operation.
     *
     * @param info  The parameter bundle specifying the details of the new list.
     * @param lists The lists to merge.
     * @return A new list
     */
    public ItemList merge(ListOperationInfo info, ItemList... lists) {
        return merge(info, Arrays.asList(lists));
    }

    /**
     * Merge one or more lists into a new list.
     *
     * This operation is the equivalent of the set 'union' operation.
     *
     * @param lists The lists to merge.
     * @return A new list
     */
    public ItemList merge(Collection<ItemList> lists) {
        return merge(new ListOperationInfo(), lists);
    }

    /**
     * Merge one or more lists into a new list.
     *
     * This operation is the equivalent of the set 'union' operation.
     *
     * @param info  The parameter bundle specifying the details of the new list.
     * @param lists The lists to merge.
     * @return A new list
     */
    public ItemList merge(ListOperationInfo info, Collection<ItemList> lists) {
        ListRequest request = new ListRequest(RequestType.POST,
                getRootUrl() + MERGE_PATH, ContentType.APPLICATION_JSON);
        return processCommutativeOperation(request, info, lists);
    }

    // -- INTERSECTIONS

    /**
     * Intersect one or more lists to produce a new list.
     *
     * @param lists The lists to intersect.
     * @return A new list
     */
    public ItemList intersect(ItemList... lists) {
        return intersect(new ListOperationInfo(), Arrays.asList(lists));
    }

    /**
     * Intersect one or more lists to produce a new list.
     *
     * @param info  The parameter bundle specifying the details of the new list.
     * @param lists The lists to intersect.
     * @return A new list
     */
    public ItemList intersect(ListOperationInfo info, ItemList... lists) {
        return intersect(info, Arrays.asList(lists));
    }

    /**
     * Intersect one or more lists to produce a new list.
     *
     * @param lists The lists to intersect.
     * @return A new list
     */
    public ItemList intersect(Collection<ItemList> lists) {
        return intersect(new ListOperationInfo(), lists);
    }

    /**
     * Intersect one or more lists to produce a new list.
     *
     * @param info  The parameter bundle specifying the details of the new list.
     * @param lists The lists to intersect.
     * @return A new list
     */
    public ItemList intersect(ListOperationInfo info, Collection<ItemList> lists) {
        ListRequest request = new ListRequest(RequestType.POST,
                getRootUrl() + INTERSECT_PATH, ContentType.APPLICATION_JSON);
        return processCommutativeOperation(request, info, lists);
    }

    // -- DIFFING

    /**
     * Find the symmetric difference of one or more lists to produce a new list.
     *
     * @param lists The lists to find the symmetric difference of.
     * @return A new list
     */
    public ItemList diff(ItemList... lists) {
        return diff(new ListOperationInfo(), Arrays.asList(lists));
    }

    /**
     * Find the symmetric difference of one or more lists to produce a new list.
     *
     * @param info  The parameter bundle specifying the details of the new list.
     * @param lists The lists to find the symmetric difference of.
     * @return A new list
     */
    public ItemList diff(ListOperationInfo info, ItemList... lists) {
        return diff(info, Arrays.asList(lists));
    }

    /**
     * Find the symmetric difference of one or more lists to produce a new list.
     *
     * @param lists The lists to find the symmetric difference of.
     * @return A new list
     */
    public ItemList diff(Collection<ItemList> lists) {
        return diff(new ListOperationInfo(), lists);
    }

    /**
     * Find the symmetric difference of one or more lists to produce a new list.
     *
     * @param info  The parameter bundle specifying the details of the new list.
     * @param lists The lists to find the symmetric difference of.
     * @return A new list
     */
    public ItemList diff(ListOperationInfo info, Collection<ItemList> lists) {
        ListRequest request = new ListRequest(RequestType.POST,
                getRootUrl() + DIFF_PATH, ContentType.APPLICATION_JSON);
        return processCommutativeOperation(request, info, lists);
    }

    // -- SUBTRACTION

    /**
     * Subtract some lists from some other lists to produce a new list.
     *
     * {@link ItemList#subtract(ListOperationInfo, ItemList...)}
     * @param info  The parameter bundle specifying the details of the new list.
     * @param from  The lists to subtract items from.
     * @param lists The lists to subtract.
     * @return A new list
     */
    public ItemList subtract(ListOperationInfo info, ItemList from, ItemList... lists) {
        return subtract(info, Arrays.asList(from), Arrays.asList(lists));
    }

    /**
     * Subtract some lists from some other lists to produce a new list.
     *
     * {@link ItemList#subtract(ListOperationInfo, ItemList...)}
     * @param info  The parameter bundle specifying the details of the new list.
     * @param from  The lists to subtract items from.
     * @param lists The lists to subtract.
     * @return A new list
     */
    public ItemList subtract(ListOperationInfo info, ItemList[] from, ItemList... lists) {
        return subtract(info, Arrays.asList(from), Arrays.asList(lists));
    }

    /**
     * Subtract some lists from some other lists to produce a new list.
     *
     * {@link ItemList#subtract(ItemList...)}
     * @param from  The list to subtract items from.
     * @param lists The lists to subtract.
     * @return A new list
     */
    public ItemList subtract(ItemList from, ItemList... lists) {
        return subtract(Arrays.asList(from), Arrays.asList(lists));
    }

    /**
     * Subtract some lists from some other lists to produce a new list.
     *
     * {@link ItemList#subtract(ItemList...)}
     * @param from  The lists to subtract items from.
     * @param lists The lists to subtract.
     * @return A new list
     */
    public ItemList subtract(ItemList[] from, ItemList... lists) {
        return subtract(Arrays.asList(from), Arrays.asList(lists));
    }

    /**
     * Subtract some lists from some other lists to produce a new list.
     *
     * {@link ItemList#subtract(Collection)}
     * @param from  The lists to subtract items from.
     * @param lists The lists to subtract.
     * @return A new list
     */
    public ItemList subtract(Collection<ItemList> from, Collection<ItemList> lists) {
        return subtract(new ListOperationInfo(), from, lists);
    }

    /**
     * Subtract some lists from some other lists to produce a new list.
     *
     * {@link ItemList#subtract(ListOperationInfo, Collection)}
     * @param info  The parameter bundle specifying the details of the new list.
     * @param x     The lists to subtract items from.
     * @param y     The lists to subtract.
     * @return A new list
     */
    public ItemList subtract(ListOperationInfo info,
            Collection<ItemList> x, Collection<ItemList> y) {
        ListRequest request = new ListRequest(RequestType.POST,
                getRootUrl() + SUBTRACT_PATH, ContentType.APPLICATION_JSON);
        applyListOperationParameters(request, info);
        request.setParameter("references", join(collect(x, invokerTransformer("getName")), ';'));
        request.setParameter("subtract", join(collect(y, invokerTransformer("getName")), ';'));
        return processListCreationRequest(request);
    }

    // -- RENAMING

    /**
     * Rename a list on the server, returning a list representing the new state.
     *
     * Use {@link ItemList#rename(String)}
     *
     * @param list The list to rename.
     * @param newName The new name to give it.
     * @return A list object representing the new state.
     */
    public ItemList rename(ItemList list, String newName) {
        ListRequest request = new ListRequest(RequestType.POST,
                getRootUrl() + RENAME_PATH, ContentType.APPLICATION_JSON);
        request.setParameter("oldname", list.getName());
        request.setParameter("newname", newName);
        return processListCreationRequest(request);
    }

    /**
     * Report whether the given list contains all the items in the
     * supplied array.
     *
     * @param list The list supposed to contain the items.
     * @param items The items supposed to belong to the list
     * @return Whether or not all the items are members of this list.
     */
    public boolean contains(ItemList list, Item... items) {
        PathQuery pq = new PathQuery(getFactory().getModel());
        pq.addView(list.getType() + ".id");
        Set<String> ids = new HashSet<String>();
        for (Item i: items) {
            ids.add(Integer.toString(i.getId()));
        }
        pq.addConstraint(Constraints.oneOfValues(list.getType() + ".id", ids));
        pq.addConstraint(Constraints.in(list.getType(), list.getName()));
        int count = getFactory().getQueryService().getCount(pq);
        return ids.size() == count;
    }

    // -- INTERNAL LOGIC

    private List<ItemList> processListsRequest(Request request) {
        HttpConnection connection = executeRequest(request);
        String body = connection.getResponseBodyAsString();
        try {
            JSONObject resultSet = new JSONObject(body);
            if (!resultSet.isNull("error")) {
                throw new ServiceException(resultSet.getString("error"));
            }
            JSONArray lists = resultSet.getJSONArray("lists");
            int length = lists.length();
            List<ItemList> ret = new ArrayList<ItemList>();
            try {
                for (int i = 0; i < length; i++) {
                    ret.add(Lists.parseList(getFactory(), lists.getJSONObject(i)));
                }
            } catch (JSONException e) {
                throw new ServiceException("Error processing request: "
                        + request + ", Incorrect JSON returned: '" + body + "'", e);
            }
            return ret;
        } catch (JSONException e) {
            throw new ServiceException("Error processing request: "
                    + request + ", error parsing list data: '" + body + "'", e);
        }
    }

    private Request getIdsToListRequest(ListCreationInfo info) {
        CreationRequest request = new CreationRequest(getRootUrl() + LISTS_PATH);
        applyListOperationParameters(request, info);
        request.setParameter("type", info.type);
        if (info.fileSrc != null) {
            try {
                request.addContents(info.fileSrc);
            } catch (FileNotFoundException e) {
                throw new ServiceException("while constructing list:", e);
            }
        } else if (info.ids != null) {
            request.addContents(info.ids);
        } else {
            throw new ServiceException("No content specified");
        }
        return request;
    }

    private Request getQueryToListRequest(ListCreationInfo info) {
        Request request = new ListRequest(RequestType.POST,
                getRootUrl() + QUERY_LISTS_PATH, ContentType.APPLICATION_JSON);
        request.setParameter("listName", info.name);
        request.setParameter("description", info.description);
        request.setParameter("query", info.querySrc.toXml());
        request.setParameter("tags", info.getTagString());
        return request;
    }

    private ItemList processCommutativeOperation(Request request,
            ListOperationInfo info, Collection<ItemList> lists) {
        applyListOperationParameters(request, info);
        request.setParameter("lists", join(collect(lists, invokerTransformer("getName")), ';'));
        return processListCreationRequest(request);
    }

    private void applyListOperationParameters(Request request, ListOperationInfo info) {
        request.setParameter("name", info.name);
        request.setParameter("description", info.description);
        request.setParameter("tags", info.getTagString());
    }

    private ItemList processListCreationRequest(Request request) {
        HttpConnection con = executeRequest(request);
        String body = con.getResponseBodyAsString();

        ItemList created = Lists.parseListCreationInfo(getFactory(), body);
        ItemList onServer = getList(created.getName());
        onServer.addUnmatchedIds(created.getUnmatchedIdentifiers());
        return onServer;
    }

    /**
     * An abstraction of the combination of options that can be specified to generate a
     * new list in a list operation.
     *
     * This object has facilities for supplying the minimum amount of information, including
     * automatically generating unused list names.
     *
     * @author Alex Kalderimis
     *
     */
    public class ListOperationInfo
    {
        private static final String DEFAULT_DESCRIPTION = "Created with Java Webservice-Client";
        private static final String BASE_NAME = "my-list";
        String type;
        String name;

        /**
         * The description of the list. By default a description will be applied
         * so that the user may more easily identify the automatically created lists.
         */
        String description = DEFAULT_DESCRIPTION;

        /** The tags for the list - by default no tags will be applied. **/
        final Set<String> tags = new HashSet<String>();

        /**
         * Create a new options bundle with all values set to their defaults.
         */
        public ListOperationInfo() {
            this.name = getUnusedListName();
        }

        /**
         * Create a new options bundle, with all elements set to their default apart
         * from the name, which is given.
         * @param name The name of the new list to create.
         */
        public ListOperationInfo(String name) {
            this.name = name;
        }

        /**
         * Create a new options bundle, with all elements set to their default apart
         * from the name and the description, which are given.
         * @param name the name of the new list to create.
         * @param description A description to apply to the list.
         */
        public ListOperationInfo(String name, String description) {
            this(name);
            this.description = description;
        }

        /**
         * Create a new options bundle, specifying all options.
         * @param name        The name of the new list to create.
         * @param description A description to apply to the list.
         * @param tags        The tags to apply to the list.
         */
        public ListOperationInfo(String name, String description,
                Collection<? extends String> tags) {
            this(name, description);
            this.tags.addAll(tags);
        }

        /**
         * Add a tag to the set of tags to apply.
         * @param tag The tag to add.
         */
        public void addTag(String tag) {
            tags.add(tag);
        }

        /**
         * Add some tags to the set of tags to apply.
         * @param tags The tags to apply.
         */
        public void addTags(Collection<? extends String> tags) {
            this.tags.addAll(tags);
        }

        /**
         * Get the tags joined in the manner required for the list service.
         * @return A string suitable for using as a value in a list service request.
         */
        public String getTagString() {
            return StringUtils.join(tags, ';');
        }

        /**
         * Get a name for the list if none has been supplied.
         * @return A new default name, which does not clash with the name of any existing
         * name the user has access to.
         */
        protected String getUnusedListName() {
            Set<String> usedNames = getListMap().keySet();
            String newName = BASE_NAME;
            int counter = 0;
            while (usedNames.contains(newName)) {
                counter++;
                newName = BASE_NAME + "-" + counter;
            }
            return newName;
        }

    }

    /**
     * An abstraction of the combination of options that can be specified to generate a
     * new list in a list creation operation.
     *
     * This object has facilities for supplying the minimum amount of information, including
     * automatically generating unused list names.
     *
     * @author Alex Kalderimis.
     */
    public class ListCreationInfo extends ListOperationInfo
    {

        String type;
        String ids;
        File fileSrc;
        PathQuery querySrc;

        /**
         * Create a new options bundle with all possible optional values set to
         * their defaults.
         * @param type The class of the objects the new list will hold. This must be a valid
         * class in the service's data-model.
         */
        public ListCreationInfo(String type) {
            super();
            this.type = type;
        }

        /**
         * Create a new options bundle, specifying the query to use to find objects with.
         *
         * @param pq A query to run to collect the objects in the new list. This query must only
         * have one output column (the object id).
         */
        public ListCreationInfo(PathQuery pq) {
            super();
            setContent(pq);
        }

        /**
         * Create a new options bundle, specifying the name.
         *
         * @param pq A query to run to collect the objects in the new list. This query must only
         * have one output column (the object id).
         * @param name The name for the new list.
         */
        public ListCreationInfo(PathQuery pq, String name) {
            super(name);
            setContent(pq);
        }

        /**
         * Create a new options bundle, with the name taken from the supplied value.
         *
         * @param type The class of the objects the new list will hold. This must be a valid
         * class in the service's data-model.
         * @param name The name for the new list.
         */
        public ListCreationInfo(String type, String name) {
            super(name);
            this.type = type;
        }

        /**
         * Create a new options bundle, specifying the name and the description.
         *
         * @param pq A query to run to collect the objects in the new list. This query must only
         * have one output column (the object id).
         * @param name The name for the new list.
         * @param description A description to apply to the new list.
         */
        public ListCreationInfo(PathQuery pq, String name, String description) {
            super(name, description);
            setContent(pq);
        }

        /**
         * Create a new options bundle, with the name and description taken
         * from the supplied value.
         *
         * @param type The class of the objects the new list will hold. This must be a valid
         * class in the service's data-model.
         * @param name The name for the new list.
         * @param description A description to apply to the new list.
         */
        public ListCreationInfo(String type, String name, String description) {
            super(name, description);
            this.type = type;
        }

        /**
         * Create a new options bundle, specifying all parameters.
         *
         * @param pq A query to run to collect the objects in the new list. This query must only
         * have one output column (the object id).
         * @param name The name for the new list.
         * @param description A description to apply to the new list.
         * @param tags A set of tags to apply to the new list.
         */
        public ListCreationInfo(PathQuery pq, String name, String description,
                Collection<? extends String> tags) {
            super(name, description, tags);
            setContent(pq);
        }

        /**
         * Create a new options bundle, specifying all parameters.
         *
         * @param type The class of the objects the new list will hold. This must be a valid
         * class in the service's data-model.
         * @param name The name for the new list.
         * @param description A description to apply to the new list.
         * @param tags A set of tags to apply to the new list.
         */
        public ListCreationInfo(String type, String name, String description,
                Collection<? extends String> tags) {
            super(name, description, tags);
            this.type = type;
        }

        /**
         * Set the content for this creation request as coming from a file on the file-system.
         * @param ids The file to get ids from.
         */
        public void setContent(File ids) {
            this.fileSrc = ids;
        }

        /**
         * Set the content for this creation request as coming from an InputStream.
         * @param is The source of the content.
         * @throws IOException If there is a problem reading from the stream.
         */
        public void setContent(InputStream is) throws IOException {
            this.ids = IOUtils.toString(is);
        }

        /**
         * Set the content for this request as coming from a set of strings, with each
         * string representing one identifier.
         *
         * Before transmission, these identifiers will be escaped and joined in such as way as
         * to ensure they are handled as individual identifiers, even if they contain spaces.
         *
         * @param ids The identifiers to use as content.
         */
        public void setContent(Collection<String> ids) {
            StringBuffer sb = new StringBuffer();
            for (String s: ids) {
                sb.append("\"" + s + "\"\n");
            }
            this.ids = sb.toString();
        }

        /**
         * Set the content for this request as coming from a set of strings, with each
         * string representing one identifier.
         *
         * Before transmission, these identifiers will be escaped and joined in such as way as
         * to ensure they are handled as individual identifiers, even if they contain spaces.
         *
         * @param identifiers The identifiers to use as content.
         */
        public void setContent(String... identifiers) {
            setContent(Arrays.asList(identifiers));
        }

        /**
         * Set the content as coming from a string. This string will be sent verbatim to the
         * server.
         * @param ids The content for the request.
         */
        public void setContent(String ids) {
            this.ids = ids;
        }

        /**
         * Set the content for this request as a query to run to find objects with.
         *
         * Note that this query must only have one output column, which must represent the
         * object id of the items you require.
         * @param pq The query to use as a source of objects for the request.
         */
        public void setContent(PathQuery pq) {
            this.querySrc = pq;
        }
    }

    /**
     * Get the most current tag set for a list.
     * @param itemList The list to get tags for.
     * @return A list of tags.
     */
    public List<String> getTags(ItemList itemList) {
        Request request = new RequestImpl(RequestType.GET, getRootUrl() + TAGS,
                ContentType.APPLICATION_JSON);
        request.setParameter("name", itemList.getName());
        return handleTagRequest(request);
    }

    /**
     * Get the tags for a list after adding some to the tag set.
     * @param itemList The list to add to.
     * @param newTags The new tags to add.
     * @return The current list of tags.
     */
    public List<String> addTags(ItemList itemList, String... newTags) {
        Request request = new RequestImpl(RequestType.POST, getRootUrl() + TAGS,
                ContentType.APPLICATION_JSON);
        request.setParameter("name", itemList.getName());
        request.setParameter("tags", StringUtils.join(newTags, ';'));
        return handleTagRequest(request);
    }

    /**
     * Get the tags for a list after removing some from its tag set.
     * @param itemList The list to remove from.
     * @param removeThese The tags to remove.
     * @return The current list of tags.
     */
    public List<String> removeTags(ItemList itemList, String... removeThese) {
        Request request = new RequestImpl(RequestType.DELETE, getRootUrl() + TAGS,
                ContentType.APPLICATION_JSON);
        request.setParameter("name", itemList.getName());
        request.setParameter("tags", StringUtils.join(removeThese, ';'));
        return handleTagRequest(request);
    }

    private List<String> handleTagRequest(Request request) {
        HttpConnection con = executeRequest(request);
        List<String> ret = new ArrayList<String>();
        String body = null;
        try {
            body = con.getResponseBodyAsString();
            JSONObject jo = new JSONObject(body);
            if (!jo.isNull("error")) {
                throw new ServiceException(jo.getString("error"));
            }
            JSONArray tags = jo.getJSONArray("tags");
            for (int i = 0; i < tags.length(); i++) {
                ret.add(tags.getString(i));
            }
        } catch (JSONException e) {
            throw new ServiceException("while parsing response: " + body, e);
        } finally {
            con.close();
        }
        return ret;
    }

}
