package org.intermine.common.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

/**
 * A generic and extended version of the AbstractListModel. The elements are sorted
 * by the comparator given at creation time.
 * 
 * @param <T> The type of items stored in this model.
 */
public class GenericMutableComboBoxModel<T>
extends AbstractListModel implements MutableComboBoxModel, Iterable<T>
{

    private static final long serialVersionUID = -4496531833767093677L;

    /**
     * The items in this model.
     * @serial
     */
    private List<T> things = new ArrayList<T>();
    
    /**
     * The index of the selected item.
     * @serial
     */
    private int selectedIndex = -1;

    /**
     * The comparator used to sort the collection.
     * @serial
     */
    private Comparator<? super T> comparator;


    /**
     * Initialise this model with the given comparator.
     * 
     * @param comparator A comparator capable of sorting collections of <code>T</code>.
     * 
     * @throws IllegalArgumentException if <code>comparator</code> is null.
     */
    public GenericMutableComboBoxModel(Comparator<? super T> comparator) {
        setComparator(comparator);
    }

    /**
     * Set the comparator sorting this model.
     * 
     * @param comparator The new comparator.
     * 
     * @throws IllegalArgumentException if <code>comparator</code> is null.
     */
    public void setComparator(Comparator<? super T> comparator) {
        if (comparator == null) {
            throw new IllegalArgumentException("comparator cannot be null");
        }
        this.comparator = comparator;
        Collections.sort(things, comparator);
        fireContentsChanged(this, 0, things.size() - 1);
    }

    /**
     * Get the item at the given index in this model.
     * 
     * @param index The index of the item to return.
     * 
     * @return The item at position <code>index</code>.
     * 
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range.
     */
    @Override
    public Object getElementAt(int index) {
        return getThingAt(index);
    }

    /**
     * Typed version of {@link #getElementAt(int)}.
     * 
     * @param index The index of the item to return.
     * 
     * @return The <code>T</code> at position <code>index</code>.
     * 
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range.
     */
    public T getThingAt(int index) {
        return things.get(index);
    }

    /**
     * Get the index of <code>thing</code> in this model.
     * 
     * @param thing The item to search for.
     * 
     * @return The index of <code>thing</code>, or -1 if it is not found.
     * 
     * @see List#indexOf(Object)
     */
    public int indexOf(Object thing) {
        return things.indexOf(thing);
    }

    /**
     * Get the number of items in this model.
     * 
     * @return The number of items.
     */
    @Override
    public int getSize() {
        return things.size();
    }

    /**
     * Test whether this model has no elements.
     * 
     * @return <code>true</code> if this model is empty, <code>false</code> if not.
     * 
     * @see Collection#isEmpty()
     */
    public boolean isEmpty() {
        return things.isEmpty();
    }

    /**
     * Get the currently selected item.
     * 
     * @return The selected item, or <code>null</code> if nothing is selected.
     */
    @Override
    public Object getSelectedItem() {
        return getSelectedThing();
    }

    /**
     * Typed version of {@link #getSelectedItem()}.
     * 
     * @return The selected item, or <code>null</code> if nothing is selected.
     */
    public T getSelectedThing() {
        return selectedIndex < 0 ? null : things.get(selectedIndex);
    }

    /**
     * Set the currently selected item.
     * 
     * @param anItem The new selected item.
     */
    @Override
    public void setSelectedItem(Object anItem) {
        selectedIndex = things.indexOf(anItem);
    }

    /**
     * Set (replace) the items in this model with those given. The items are
     * sorted using the currently set comparator.
     * 
     * @param items A collection of <code>T</code> objects to add.
     */
    public void set(Collection<? extends T> items) {
        if (items != null) {
            int sizeThen = things.size();
            int sizeNow = items.size();
            things = new ArrayList<T>(items);
            Collections.sort(things, comparator);
            selectedIndex = -1;
            fireContentsChanged(this, 0, Math.max(sizeThen, sizeNow));
        }
    }

    /**
     * Add an element to this model.
     * 
     * @param obj The item to add.
     */
    @Override
    public void addElement(Object obj) {
        @SuppressWarnings("unchecked")
        T t = (T) obj;
        int index = 0;
        for (T t2 : things) {
            if (comparator.compare(t, t2) <= 0) {
                break;
            }
            index++;
        }
        things.add(index, t);
        if (selectedIndex >= index) {
            ++selectedIndex;
        }
        fireIntervalAdded(this, index, index);
    }

    /**
     * Add an element at a specified position in this model.
     * 
     * @param obj The item to add.
     * @param index The position to add the item. This is ignored, as this
     * model maintains its own sorting.
     */
    @Override
    public void insertElementAt(Object obj, int index) {
        addElement(obj);
    }

    /**
     * Remove the given element from the model.
     * 
     * @param obj The object to remove.
     */
    @Override
    public void removeElement(Object obj) {
        @SuppressWarnings("unchecked")
        T t = (T) obj;
        int index = things.indexOf(t);
        if (index >= 0) {
            things.remove(obj);
            if (selectedIndex == index) {
                selectedIndex = -1;
            } else if (selectedIndex > index) {
                --selectedIndex;
            }
            fireIntervalRemoved(this, index, index);
        }
    }

    /**
     * Remove the element at the given index from the model.
     * 
     * @param index The index of the element to remove.
     * 
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range.
     * 
     * @see List#remove(int)
     */
    @Override
    public void removeElementAt(int index) {
        things.remove(index);
        if (selectedIndex == index) {
            selectedIndex = -1;
        }
        fireIntervalRemoved(this, index, index);
    }

    /**
     * Remove all items from this model.
     */
    public void clear() {
        if (!things.isEmpty()) {
            int sizeThen = things.size();
            things.clear();
            selectedIndex = -1;
            fireIntervalRemoved(this, 0, sizeThen);
        }
    }

    /**
     * Called to indicate that the given <code>T</code> has been altered,
     * and may need to be repositioned in the model.
     * 
     * @param thing The <code>T</code> that has changed.
     * 
     * @return <code>true</code> if the change to <code>thing</code> has
     * resulted in it moving in the model, <code>false</code> if not.
     */
    public boolean thingChanged(T thing) {
        int insertIndex = 0;
        for (int searchIndex = 0; searchIndex < things.size(); searchIndex++) {

            T existing = things.get(searchIndex);
            if (existing == thing) {
                // Don't increase insert position if it's the same subcategory. No change
                // will be needed if it's new position is the same as the current.
                continue;
            }
            if (comparator.compare(thing, existing) < 0) {
                // Insert here.
                break;
            }
            insertIndex++;
        }

        int currentIndex = things.indexOf(thing);

        boolean moved = currentIndex != insertIndex;
        if (moved) {
            removeElementAt(currentIndex);
            addElement(thing);
        }
        return moved;
    }

    /**
     * Obtain an Iterator for the items in this model.
     * 
     * @return A typed Iterator.
     */
    @Override
    public Iterator<T> iterator() {
        return new ListModelIterator();
    }

    /**
     * Obtain a ListIterator for the items in this model.
     * 
     * @return A typed ListIterator.
     */
    public ListIterator<T> listIterator() {
        return new ListModelIterator();
    }

    /**
     * ListIterator implementation for the list within the outer
     * GenericMutableComboBoxModel. Removals are permitted and raise the
     * appropriate events. Additions and changes are not.
     */
    private class ListModelIterator implements ListIterator<T>
    {
        /**
         * The internal ListIterator over the parent's <code>rows</code> list.
         */
        private ListIterator<T> realIterator;
        
        /**
         * The object currently indicated by <code>realIterator</code>.
         */
        private T current;

        /**
         * Create a new ListModelIterator, indicating before the start of the things list.
         */
        public ListModelIterator() {
            realIterator = things.listIterator();
        }

        /**
         * Test whether there is an element following the current element of the iterator.
         * 
         * @return <code>true</code> if there is a subsequent element, <code>false</code>
         * if not.
         * 
         * @see ListIterator#hasNext()
         */
        public boolean hasNext() {
            return realIterator.hasNext();
        }

        /**
         * Get the index of the next object indicated by this iterator.
         * 
         * @return The index of the next object.
         * 
         * @see ListIterator#nextIndex()
         */
        public int nextIndex() {
            return realIterator.nextIndex();
        }

        /**
         * Get the next element following the current element of the iterator.
         * 
         * @return The subsequent element.
         * 
         * @throws java.util.NoSuchElementException if there is no following element.
         * 
         * @see ListIterator#next()
         */
        public T next() {
            current = realIterator.next();
            return current;
        }

        /**
         * Remove the element currently indicated by this iterator from the model.
         * <p>Raises the appropriate <code>ListDataEvent</code> for the deletion
         * for the parent model.</p>
         * 
         * @see GenericMutableComboBoxModel#removeRow(Object)
         */
        public void remove() {
            int index = -1;
            if (current != null) {
                index = things.indexOf(current);
            }

            realIterator.remove();
            current = null;

            if (index >= 0) {
                fireIntervalRemoved(GenericMutableComboBoxModel.this, index, index);
            }
        }

        /**
         * Add an object to the model via this iterator. This operation is not supported.
         * 
         * @param e The object to add.
         * 
         * @throws UnsupportedOperationException always, as this is not supported.
         */
        public void add(T e) {
            throw new UnsupportedOperationException(
                    "add(T) is not supported on GenericMutableComboBoxModels. "
                    + "Use add(T) on the model itself.");
        }

        /**
         * Check whether there are rows prior to the one currently indicated by this
         * iterator.
         * 
         * @return <code>true</code> if there is a preceding row, <code>false</code> if not.
         * 
         * @see ListIterator#hasPrevious()
         */
        public boolean hasPrevious() {
            return realIterator.hasPrevious();
        }

        /**
         * Get the index of the previous object indicated by this iterator.
         * 
         * @return The index of the previous object.
         * 
         * @see ListIterator#previousIndex()
         */
        public int previousIndex() {
            return realIterator.previousIndex();
        }

        /**
         * Get the element preceding the current element of the iterator.
         * 
         * @return The prior element.
         * 
         * @throws java.util.NoSuchElementException if there is no preceding element.
         * 
         * @see ListIterator#previous()
         */
        public T previous() {
            current = realIterator.previous();
            return current;
        }

        /**
         * Replace the current object with that given. This operation is not supported.
         * 
         * @param e The replacement object.
         * 
         * @throws UnsupportedOperationException always, as this is not supported.
         */
        public void set(T e) {
            throw new UnsupportedOperationException(
                    "set(T) is not supported on GenericMutableComboBoxModels.");
        }

    }
}
