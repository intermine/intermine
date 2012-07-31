package org.intermine.install.swing.source;

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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractListModel;

import org.intermine.modelviewer.project.Source;


/**
 * A list model for the list of source types.
 */
public class SourceListModel extends AbstractListModel implements Iterable<Source>
{
    private static final long serialVersionUID = 6067523121947054893L;

    /**
     * The sources in the list.
     * @serial
     */
    private List<Source> sources = new ArrayList<Source>();

    /**
     * Constructor.
     */
    public SourceListModel() {
    }

    /**
     * Get the source at the given index.
     * 
     * @param index The index of the source required.
     * 
     * @return The source at the given index.
     */
    @Override
    public Source getElementAt(int index) {
        return sources.get(index);
    }
    
    /**
     * Get the number of sources in the model.
     * 
     * @return The number of sources.
     */
    @Override
    public int getSize() {
        return sources.size();
    }

    /**
     * Remove all sources from the model.
     */
    public void clear() {
        int size = sources.size();
        if (size > 0) {
            sources.clear();
            fireIntervalRemoved(this, 0, size - 1);
        }
    }
    
    /**
     * Replace the current model contents with the given collection
     * of sources.
     * 
     * @param newSources The new sources.
     */
    public void setSources(Collection<Source> newSources) {
        if (newSources != null) {
            sources.clear();
            sources.addAll(newSources);
            fireContentsChanged(this, 0, sources.size() - 1);
        }
    }
    
    /**
     * Add a source to the end of the model.
     * 
     * @param s The source to add.
     */
    public void addSource(Source s) {
        addSource(s, sources.size());
    }
    
    /**
     * Add a source at the given position in the model.
     * 
     * @param s The source to add.
     * @param pos The position to add <code>s</code> at.
     */
    public void addSource(Source s, int pos) {
        if (s != null) {
            sources.add(pos, s);
            fireIntervalAdded(this, pos, pos);
        }
    }
    
    /**
     * Remove the given source.
     * 
     * @param s The source to remove.
     */
    public void removeSource(Source s) {
        if (s != null) {
            int index = sources.indexOf(s);
            if (index >= 0) {
                sources.remove(index);
                fireIntervalRemoved(this, index, index);
            }
        }
    }
    
    /**
     * Get the index of the given source in the model.
     * 
     * @param s The source.
     * 
     * @return The index of the source, or -1 if <code>s</code> is not
     * in the model.
     */
    public int indexOf(Source s) {
        return sources.indexOf(s);
    }
    
    /**
     * Get an iterator over the sources.
     * @return An iterator.
     */
    @Override
    public Iterator<Source> iterator() {
        return listIterator();
    }
    
    /**
     * Get a list iterator over the sources.
     * @return A list iterator.
     */
    public ListIterator<Source> listIterator() {
        return new SourceListIterator();
    }

    /**
     * Get a list iterator over the sources, starting at the given index.
     * 
     * @param start The position to start the iterator at.
     * 
     * @return A list iterator.
     */
    public ListIterator<Source> listIterator(int start) {
        return new SourceListIterator(start);
    }

    /**
     * A list iterator implementation to iterate over the elements in the
     * SourceListModel that fires the relevant <code>ListDataEvent</code>s
     * if items are changed or removed.
     */
    private class SourceListIterator implements ListIterator<Source>
    {
        /**
         * The underlying ListIterator.
         */
        private ListIterator<Source> myIter;
        
        /**
         * Initialise with the iterator indicating the start of the model.
         */
        SourceListIterator() {
            myIter = sources.listIterator();
        }
        
        /**
         * Initialise with the iterator indicating the given position in 
         * the model.
         * 
         * @param start The start index.
         */
        SourceListIterator(int start) {
            myIter = sources.listIterator(start);
        }
        
        /**
         * Add the given Source at the current iterator position.
         * Fires the relevant <code>ListDataEvent</code>.
         * 
         * @param s The Source to add.
         */
        @Override
        public void add(Source s) {
            int pos = myIter.previousIndex() + 1;
            myIter.add(s);
            fireIntervalAdded(SourceListModel.this, pos, pos);
        }

        /**
         * Test whether the iterator has any elements remaining.
         * 
         * @return <code>true</code> if any elements remain, <code>false</code>
         * if not.
         */
        @Override
        public boolean hasNext() {
            return myIter.hasNext();
        }

        /**
         * Test whether the iterator has any prior elements remaining.
         * 
         * @return <code>true</code> if any prior elements remain, <code>false</code>
         * if not.
         */
        @Override
        public boolean hasPrevious() {
            return myIter.hasPrevious();
        }

        /**
         * Get the next Source in the model.
         * @return The next Source.
         */
        @Override
        public Source next() {
            return myIter.next();
        }

        /**
         * Get the index of the next element from the current iterator position.
         * 
         * @return The index of the next element.
         */
        @Override
        public int nextIndex() {
            return myIter.nextIndex();
        }

        /**
         * Get the previous Source in the model.
         * @return The previous Source.
         */
        @Override
        public Source previous() {
            return myIter.previous();
        }

        /**
         * Get the index of the previous element from the current iterator position.
         * 
         * @return The index of the previous element.
         */
        @Override
        public int previousIndex() {
            return myIter.previousIndex();
        }

        /**
         * Remove the currently indicated source from the model.
         * Fires the relevant <code>ListDataEvent</code>.
         */
        @Override
        public void remove() {
            int pos = myIter.previousIndex() + 1;
            myIter.remove();
            fireIntervalRemoved(SourceListModel.this, pos, pos);
        }

        /**
         * Replace the currently indicated source with that given.
         * Fires the relevant <code>ListDataEvent</code>.
         * 
         * @param s The replacement source.
         */
        @Override
        public void set(Source s) {
            int pos = myIter.previousIndex() + 1;
            myIter.set(s);
            fireContentsChanged(SourceListModel.this, pos, pos);
        }
    }
}
