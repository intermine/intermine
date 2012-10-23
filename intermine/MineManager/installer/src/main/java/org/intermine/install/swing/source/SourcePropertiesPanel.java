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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.install.project.source.PropertyDescriptor;
import org.intermine.install.project.source.SourceInfo;
import org.intermine.install.project.source.SourceInfoLoader;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Property;
import org.intermine.modelviewer.project.Source;


/**
 * A panel for displaying the source specific property fields.
 */
public class SourcePropertiesPanel extends JPanel implements Scrollable
{
    private static final long serialVersionUID = -8760544765304724319L;

    /**
     * The margin to put around this panel's minimum size.
     */
    private static final int MIN_SIZE_MARGIN = 80;
    
    /**
     * Logger.
     */
    private transient Log logger = LogFactory.getLog(getClass());
    
    /**
     * The source being edited.
     */
    private transient Source source;
    
    /**
     * The names of the properties being displayed.
     */
    private transient SortedSet<String> propertyNames = new TreeSet<String>();
    
    /**
     * A map of property names to the project descriptor.
     */
    private transient Map<String, PropertyDescriptor> propertyInfo =
        new HashMap<String, PropertyDescriptor>();
    
    /**
     * A map of property names to Property objects.
     */
    private transient Map<String, Property> sourceProperties = new HashMap<String, Property>();
    
    /**
     * An empty panel to allow this panel to resize correctly.
     * @serial
     */
    private JPanel spacerPanel = new JPanel();
    
    /**
     * The property wrappers for the displayed fields.
     * @serial
     */
    private PropertyComponentWrapper[] propertyWrappers;

    
    /**
     * Constructor.
     */
    public SourcePropertiesPanel() {
        init();
    }

    /**
     * Common initialisation: sets the layout manager. 
     */
    private void init() {
        setLayout(new GridBagLayout());
    }
    
    /**
     * Set the source to edit. Replaces the components displayed in the panel with new
     * ones for the properties of the given source.
     * 
     * @param projectHome The project home directory.
     * @param project The Project.
     * @param s The Source.
     */
    public void setSource(File projectHome, Project project, Source s) {
        source = s;

        SourceInfo sourceInfo = null;
        if (source != null) {
            String sourceType = source.getType();
            sourceInfo = SourceInfoLoader.getInstance().getSourceInfo(sourceType);
            if (sourceInfo == null) {
                try {
                    sourceInfo =
                        SourceInfoLoader.getInstance().findDerivedSourceInfo(
                                sourceType, project, projectHome);
                    
                    if (sourceInfo == null) {
                        logger.warn("There is no source type information for the type "
                                    + sourceType);
                    }
                } catch (IOException e) {
                    logger.warn("IOException while searching for derived type information for "
                                + sourceType, e);
                    /*
                    String message = Messages.getMessage("sourcetype.loadfail.title", sourceType);
                    logger.error(message, e);
                    JOptionPane.showMessageDialog(this, message,
                            Messages.getMessage("sourcetype.loadfail.title"),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                    */
                }
            }
        }
        
        propertyNames.clear();
        propertyInfo.clear();
        sourceProperties.clear();
        
        if (sourceInfo != null) {
            for (PropertyDescriptor p : sourceInfo.getSource().getProperty()) {
                propertyNames.add(p.getName());
                propertyInfo.put(p.getName(), p);
            }
        }
        
        if (source != null) {
            for (Property p : source.getProperty()) {
                propertyNames.add(p.getName());
                sourceProperties.put(p.getName(), p);
            }
        }
        
        int numProperties = propertyNames.size();
        
        removeAll();
        setPreferredSize(null);
        
        GridBagConstraints cons = GridBagHelper.defaultConstraints();
        
        propertyWrappers = new PropertyComponentWrapper[numProperties];
        
        Iterator<String> propIter = propertyNames.iterator();
        for (int index = 0; propIter.hasNext(); index++) {
            String propertyName = propIter.next();
            
            PropertyDescriptor descriptor = propertyInfo.get(propertyName);
            Property property = sourceProperties.get(propertyName);
            
            cons.gridx = 0;
            cons.gridy = index;
            cons.gridwidth = 1;
            cons.weightx = 0;
            add(new JLabel(propertyName), cons);
            
            boolean locationProperty = isLocation(descriptor, property);
            String value = null;
            if (property != null) {
                if (locationProperty) {
                    value = property.getLocation();
                } else {
                    value = property.getValue();
                }
            }
            
            PropertyComponentWrapper wrapper =
                PropertyComponentCreator.createComponentFor(propertyName, descriptor, value);
            propertyWrappers[index] = wrapper;
            
            cons.gridx = 1;
            cons.gridwidth = GridBagConstraints.REMAINDER;
            cons.weightx = 1;
            add(wrapper.getDisplayComponent(), cons);
        }
        
        // To provide flexible space.
        cons.gridx = 0;
        cons.gridy = numProperties;
        cons.weightx = 1;
        cons.weighty = 1;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.fill = GridBagConstraints.BOTH;
        add(spacerPanel, cons);
        
        revalidate();
        Dimension min = getMinimumSize();
        Dimension pref = getPreferredSize();
        setPreferredSize(new Dimension(min.width + MIN_SIZE_MARGIN, pref.height));
    }
    
    /**
     * Save the values in the fields in this panel into the Source object.
     * 
     * @return Whether any of the source's properties have been changed.
     */
    public boolean saveChanges() {
        boolean changed = false;
        if (source != null) {
            for (PropertyComponentWrapper propWrapper : propertyWrappers) {
                String propName = propWrapper.getPropertyName();
                PropertyDescriptor descriptor = propertyInfo.get(propName);
                
                int indexOfProperty = -1;
                Property property = null;
                Iterator<Property> propIter = source.getProperty().iterator();
                for (int i = 0; propIter.hasNext(); i++) {
                    Property p = propIter.next();
                    if (p.getName().equals(propName)) {
                        indexOfProperty = i;
                        property = p;
                        break;
                    }
                }
                
                boolean locationProperty = isLocation(descriptor, property);
                String value = propWrapper.getValue();
                
                if (StringUtils.isEmpty(value)) {
                    if (property != null) {
                        // Remove the property if it is unset.
                        source.getProperty().remove(indexOfProperty);
                        changed = true;
                    }
                } else {
                    EqualsBuilder equal = new EqualsBuilder();

                    if (property == null) {
                        // Can need addition if a hand-edited file misses this
                        // property in its source.
                        
                        property = new Property();
                        property.setName(propName);
                        source.getProperty().add(property);
                        changed = true;
                    }
                    
                    if (locationProperty) {
                        equal.append(value, property.getLocation());
                        property.setLocation(value);
                        property.setValue(null);
                    } else {
                        equal.append(value, property.getValue());
                        property.setValue(value);
                        property.setLocation(null);
                    }
                    
                    changed = changed || !equal.isEquals();
                }
            }
        }
        return changed;
    }
    
    /**
     * Check whether the given property is a type of property where the value
     * goes into the <i>location</i> attribute.
     * 
     * @param pd The property descriptor.
     * @param p The property being set.
     * 
     * @return <code>true</code> if the property is a <i>location</i> property,
     * <code>false</code> if it is a <i>value</i> property.
     */
    protected boolean isLocation(PropertyDescriptor pd, Property p) {
        boolean location = false;
        if (pd != null) {
            switch (pd.getType()) {
                case FILE:
                case DIRECTORY:
                    location = true;
                    break;
            }
        } else {
            location = StringUtils.isNotEmpty(p.getLocation());
        }
        return location;
    }
    
    
    /**
     * Gets the preferred size for this panel's viewport. This will be the preferred
     * size of the panel.
     *  
     * @return The viewport's preferred size.
     * 
     * @see Scrollable#getPreferredScrollableViewportSize
     */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /**
     * Get the increment for scrolling this panel by block.
     * 
     * @param visibleRect The view area visible in the viewport.
     * @param orientation The orientation.
     * @param direction The direction of scrolling.
     * 
     * @return The amount to scroll
     * 
     * @see Scrollable#getScrollableBlockIncrement
     */
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        switch(orientation) {
            case SwingConstants.VERTICAL:
                return visibleRect.height;
            case SwingConstants.HORIZONTAL:
                return visibleRect.width;
            default:
                throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }

    /**
     * Indicates whether this panel resizes vertically as the viewport resizes.
     * 
     * @return <code>false</code>.
     * 
     * @see Scrollable#getScrollableTracksViewportHeight
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * Indicates whether this panel resizes horizontally as the viewport resizes.
     * The panel will resize with the viewport until a minimum width is reached,
     * at which point the horizontal scroll bar will appear.
     * 
     * @return <code>true</code> until the minimum size is reached, <code>false</code>
     * when under that size.
     * 
     * @see Scrollable#getScrollableTracksViewportWidth
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (getParent() instanceof JViewport) {
            JViewport parent = (JViewport) getParent();
            return parent.getWidth() >= getMinimumSize().width + MIN_SIZE_MARGIN;
        }
        return true;
    }

    /**
     * Get the increment for scrolling this panel by unit.
     * 
     * @param visibleRect The view area visible in the viewport.
     * @param orientation The orientation.
     * @param direction The direction of scrolling.
     * 
     * @return The amount to scroll
     * 
     * @see Scrollable#getScrollableUnitIncrement
     */
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        switch(orientation) {
            case SwingConstants.VERTICAL:
                return visibleRect.height / 10;
            case SwingConstants.HORIZONTAL:
                return visibleRect.width / 10;
            default:
                throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }
}

