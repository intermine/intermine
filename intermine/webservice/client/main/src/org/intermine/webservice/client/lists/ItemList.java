package org.intermine.webservice.client.lists;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.collections.TransformerUtils.invokerTransformer;
import static org.apache.commons.collections.TransformerUtils.stringValueTransformer;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.results.Item;
import org.intermine.webservice.client.services.ListService.ListCreationInfo;
import org.intermine.webservice.client.services.ListService.ListOperationInfo;
import org.intermine.webservice.client.util.PQUtil;

/**
 * A representation of a list of objects stored on the server.
 *
 * The object is not meant to be directly instantiated nor modified. Rather it
 * should be retrieved from the originating web-service, and only modified through
 * the available methods (appending new items, and renaming the list).
 *
 * See {@link InterMineBag}
 * @author Alex Kalderimis
 */
public class ItemList extends AbstractSet<Item> implements Iterable<Item>
{

    private final ServiceFactory services;
    private String name;
    private int size;
    private String type;
    private String description;
    private String status;
    private List<String> tags;
    private boolean authorized;
    private Date createdAt;
    private Set<String> unmatchedIds = new HashSet<String>();
    private List<Item> items = null;

    /**
     * Get the identifiers used in creating or appending to this list that
     * could not be resolved to any objects. This set will be updated
     * whenever new requests are made to append to this list.
     * @return A set of unmatched identifiers.
     */
    public Set<String> getUnmatchedIdentifiers() {
        return new HashSet<String>(unmatchedIds);
    }

    /**
     * Get the date when this list was created.
     * @return The list's creation date.
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Add a new unmatched id to the set of unmatched identifiers.
     *
     * This is meant for use by the service modules.
     * @param id The id to add.
     */
    protected void addUnmatchedId(String id) {
        unmatchedIds.add(id);
    }

    /**
     * Add some new unmatched ids to the set of unmatched identifiers.
     *
     * This is meant for use by the service modules.
     * @param ids the ids to add.
     */
    public void addUnmatchedIds(Collection<String> ids) {
        unmatchedIds.addAll(ids);
    }

    /**
     * @return Whether the current user has the authority to modify this list.
     */
    public boolean isAuthorized() {
        return authorized;
    }

    /**
     * Modifying the returned set will not change the underlying tags collection.
     * @return the tags this list has.
     */
    public Set<String> getTags() {
        return new HashSet<String>(tags);
    }

    /**
     * Make sure that the tags on this object are up-to-date with those
     * stored on the server.
     */
    public void updateTags() {
        tags = services.getListService().getTags(this);
    }

    /**
     * Add some new tags to this list. After adding, list will have an
     * updated set of tags, and the tags will be stored on the server.
     * @param newTags The tags to add.
     */
    public void addTags(String... newTags) {
        tags = services.getListService().addTags(this, newTags);
    }

    /**
     * Remove some tags from this list. After removing, the list will have
     * an updated set of tags, and the tags will be synchronised with those on
     * the server.
     * @param removeThese The tags to remove.
     */
    public void removeTags(String... removeThese) {
        tags = services.getListService().removeTags(this, removeThese);
    }

    /**
     * Get the name of this list.
     * @return The list's name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the number of items in this list.
     */
    public int getSize() {
        return size;
    }

    /**
     * Synonym for getSize()
     * @return the size of the list.
     **/
    @Override
    public int size() {
        return getSize();
    }

    /**
     * @return return the type of the list.
     */
    public String getType() {
        return type;
    }

    /**
     * @return The description of the list, or null.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Any status other than <code>CURRENT</code>
     * indicated this list needs manual attention in the
     * webapp.
     * @return the current status if the list.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Construct the ItemList object.
     *
     * @param fac A reference to the ServiceFactory for the web-service.
     * @param name The name of the list.
     * @param desc The list's description.
     * @param size The number of objects in the list.
     * @param type The class of the objects in the list.
     * @param tags The tags this list is associated with.
     * @param authorized Whether the user can modify this list.
     * @param createdAt When the list was created.
     * @param status The upgrade status of the list.
     */
    ItemList(ServiceFactory fac, String name, String desc, int size,
            String type, List<String> tags, boolean authorized, Date createdAt, String status) {
        assert (name != null);
        assert (type != null);
        this.services = fac;
        this.name = name;
        this.description = desc;
        this.size = size;
        this.type = type;
        this.tags = tags;
        this.authorized = authorized;
        this.createdAt = createdAt;
        this.status = status;
    }

    @Override
    public String toString() {
        return name + " ("  + description + " - " + size + " " + type
                + "s) created at: " + createdAt + ", tags: " + tags
                + ", authorized: " + authorized;
    }

    private List<Item> getItems() {
        if (items == null) {
            items = new ArrayList<Item>();
            for (Item i: this) {
                items.add(i);
            }
        }
        return items;
    }

    /**
     * Get an item by its index. Note that while the order of elements is
     * determinate, it is difficult to predict what that order may be.
     *
     * @param index The index of the element to get.
     * @return an Item representing one of the elements in the list.
     */
    public Item get(int index) {
        return getItems().get(index);
    }

    private void update(ItemList updated) {
        items = null;
        this.size = updated.size();
        this.unmatchedIds.addAll(updated.getUnmatchedIdentifiers());
    }

    /**
     * Add new items to the list by resolving the identifiers of objects.
     *
     * This method will update the size of the list, and clear the items cache.
     * @param ids The identifiers to use to find objects to add.
     */
    public void append(String... ids) {
        update(services.getListService().append(this, ids));
    }

    /**
    * Add new items to the list by resolving the identifiers of objects.
    *
    * This method will update the size of the list, and clear the items cache.
    * @param ids The identifiers to use to find objects to add.
    **/
    public void append(Collection<? extends String> ids) {
        update(services.getListService().append(this, ids.toArray(new String[ids.size()])));
    }

    /**
     * Add new items to the list.
     *
     * This method will update the size of the list, and clear the items cache.
     * @param items The items to add.
     **/
    public void append(Item... items) {
        appendItems(Arrays.asList(items));
    }

    /**
     * Add new items to the list by resolving the identifiers of objects.
     *
     * This method will update the size of the list, and clear the items cache.
     * @param pq The query to run to find objects with.
     **/
    public void append(PathQuery pq) {
        update(services.getListService().append(this, pq));
    }

    @Override
    public boolean add(Item i) {
        return addAll(Arrays.asList(i));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean addAll(@SuppressWarnings("rawtypes") Collection items) {
        int priorSize = getSize();
        appendItems(items);
        return priorSize != getSize();
    }

    /**
     * Add items to this list by using a query.
     * @param items The items to add to the list.
     */
    private void appendItems(Iterable<Item> items) {
        PathQuery pq = new PathQuery(services.getModel());
        pq.addViews(getType() + ".id");
        Set<String> values = new HashSet<String>();
        for (Item i: items) {
            values.add(Integer.toString(i.getId()));
        }
        pq.addConstraint(Constraints.oneOfValues(getType() + ".id", values));
        update(services.getListService().append(this, pq));
    }

    // -- SUBTRACTION

    @Override
    public boolean remove(Object i) {
        int priorSize = getSize();
        if (i instanceof Item) {
            Item item = (Item) i;
            String path = item.getType() + ".id";
            PathQuery pq = new PathQuery(services.getModel());
            pq.addView(path);
            pq.addConstraint(Constraints.eq(path, Integer.toString(item.getId())));
            createListAndSubtract(pq);
        }
        return priorSize != getSize();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean removeAll(@SuppressWarnings("rawtypes") Collection c) {
        int priorSize = getSize();
        if (c instanceof ItemList) {
            ItemList res = subtract(((ItemList) c));
            update(res);
            delete();
            res.rename(getName());
        } else if (!c.isEmpty()) {
            try {
                Item[] is = (Item[]) c.toArray(new Item[0]);
                String path = is[0].getType() + ".id";
                PathQuery pq = new PathQuery(services.getModel());
                pq.addView(path);
                pq.addConstraint(Constraints.oneOfValues(path, collect(
                                collect(Arrays.asList(is), invokerTransformer("getId")),
                                stringValueTransformer())));
                createListAndSubtract(pq);
            } catch (ArrayStoreException e) {
                // Do nothing - we didn't get a collection of items.
            }
        }
        return priorSize != getSize();
    }

    private void createListAndSubtract(PathQuery pq) {
        ListCreationInfo info = services.getListService().new ListCreationInfo(pq);
        ItemList removalList = null;
        try {
            removalList = services.getListService().createList(info);
            ItemList res = subtract(removalList);
            update(res);
            delete();
            res.rename(getName());
        } finally {
            try {
                removalList.delete();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Call for this list to be deleted on the server. As soon as the call returns,
     * this list should no longer be used for any operation.
     */
    public void delete() {
        services.getListService().deleteList(this);
    }

    /**
     * Remove the items in the given lists from this list.
     *
     * This call performs the operation of asymmetric difference on the current list
     * and returns the result. For operations that alter the
     * current list see {@link #removeAll(Collection)}
     * @param lists The lists to subtract from this list.
     * @return A new list
     */
    public ItemList subtract(ItemList... lists) {
        return services.getListService().subtract(
                Collections.singleton(this), Arrays.asList(lists));
    }

    /**
     * Remove the items in the given lists from this list.
     *
     * This call performs the operation of asymmetric difference on the current list
     * and returns the result. For operations that alter the
     * current list see {@link #removeAll(Collection)}
     * @param lists The lists to subtract from this list.
     * @param info The options describing the new list.
     * @return A new list
     */
    public ItemList subtract(ListOperationInfo info, ItemList... lists) {
        return services.getListService().subtract(info,
                Collections.singleton(this), Arrays.asList(lists));
    }

    /**
     * Remove the items in the given lists from this list.
     *
     * This call performs the operation of asymmetric difference on the current list
     * and returns the result. For operations that alter the
     * current list see {@link #removeAll(Collection)}
     * @param lists The lists to subtract from this list.
     * @return A new list
     */
    public ItemList subtract(Collection<ItemList> lists) {
        return services.getListService().subtract(Collections.singleton(this), lists);
    }

    /**
     * Remove the items in the given lists from this list.
     *
     * This call performs the operation of asymmetric difference on the current list
     * and returns the result. For operations that alter the
     * current list see {@link #removeAll(Collection)}
     * @param lists The lists to subtract from this list.
     * @param info The options describing the new list.
     * @return A new list
     */
    public ItemList subtract(ListOperationInfo info, Collection<ItemList> lists) {
        return services.getListService().subtract(info, Collections.singleton(this), lists);
    }

    // -- RENAMING

    /**
     * Rename this list on the server.
     *
     * This method will update the name of the list stored on the server, and
     * also change of the name returned by this object.
     * @param newName The new name to give this list.
     */
    public void rename(String newName) {
        ItemList updated = services.getListService().rename(this, newName);
        this.name = updated.getName();
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Item) {
            Item i = (Item) o;
            return containsAll(Arrays.asList(i));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsAll(@SuppressWarnings("rawtypes") Collection c) {
        if (c == null) {
            return false;
        }
        Item[] candidates;
        try {
            candidates = (Item[]) c.toArray(new Item[c.size()]);
        } catch (ArrayStoreException e) {
            return false;
        }
        return services.getListService().contains(this, candidates);
    }

    /**
     * Convenience method for finding members of a collection matching certain properties.
     *
     * This search is implemented with a query that performs case-insensitive matching on values.
     * Null constraints will be honoured, but other lookups will be converted to strings. Wild-cards
     * are permitted wherever they are permitted in path-queries.
     * <br/>
     * Using a query to back the search means that finding matching members is efficient even over
     * large lists - you will not need to pull in and iterate over thousands of rows of data to find
     * items if you have specific conditions.
     * <br/>
     * These conditions must all match for the result to be included. For more specific and flexible
     * searching strategies, please see the {@link PathQuery} API.
     *
     * @param conditions The properties these elements should have.
     * @return A list of matching items.
     */
    public List<Item> find(Map<String, Object> conditions) {
        List<Item> ret = new ArrayList<Item>();
        PathQuery q = new PathQuery(services.getModel());
        q.addViews(PQUtil.getStar(services.getModel(), type));
        q.addConstraint(Constraints.in(type, name));
        for (Entry<String, Object> condition: conditions.entrySet()) {
            String path = type + "." + condition.getKey();
            if (condition.getValue() == null) {
                q.addConstraint(Constraints.isNull(path));
            } else {
                q.addConstraint(Constraints.eq(path, condition.getValue().toString()));
            }
        }
        List<Map<String, Object>> results = services.getQueryService().getRowsAsMaps(q);
        for (Map<String, Object> result: results) {
            ret.add(new Item(services, type, result));
        }
        return ret;
    }

    @Override
    public Iterator<Item> iterator() {
        PathQuery pq = new PathQuery(services.getModel());
        pq.addViews(PQUtil.getStar(services.getModel(), getType()));
        pq.addConstraint(Constraints.in(type, name));
        Iterator<Map<String, Object>> it = services.getQueryService().getRowMapIterator(pq);
        return new ItemIterator(it);
    }

    private class ItemIterator implements Iterator<Item>
    {

        Iterator<Map<String, Object>> results;

        ItemIterator(Iterator<Map<String, Object>> results) {
            this.results = results;
        }

        @Override
        public boolean hasNext() {
            return results.hasNext();
        }

        @Override
        public Item next() {
            return new Item(services, type, results.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("removal is not supported");
        }
    }

}
