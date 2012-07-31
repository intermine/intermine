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

import java.awt.Component;
import java.util.ResourceBundle;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 * A renderer for enumerations in a JComboBox. The strings displayed read from
 * a supplied ResourceBundle.
 * 
 * <p>The keys for the messages are the lower case enumeration values,
 * optionally with a prefix.</p>
 * 
 * @param <T> The type of enumeration.
 */
public class EnumComboRenderer<T extends Enum<T>> extends DefaultListCellRenderer
{
    private static final long serialVersionUID = -85561498140236233L;

    /**
     * The resource bundle containing the display messages for each item of the
     * enumeration.
     * @serial
     */
    private ResourceBundle messages;
    
    /**
     * The prefix for the keys in the message bundle.
     * @serial
     */
    private String messagePrefix;

    
    /**
     * Create a new renderer.
     * 
     * @param messages The resource bundle containing the messages.
     * @param messagePrefix An option prefix for the keys in the message bundle.
     */
    public EnumComboRenderer(ResourceBundle messages, String messagePrefix) {
        if (messages == null) {
            throw new IllegalArgumentException("messages cannot be null");
        }
        this.messages = messages;
        this.messagePrefix = messagePrefix;
    }

    /**
     * Determine the display value for plotting.
     * 
     * @param list The JList we're painting.
     * @param value The value returned by list.getModel().getElementAt(index).
     * @param index The cell's index.
     * @param isSelected True if the specified cell was selected.
     * @param cellHasFocus True if the specified cell has the focus.
     * 
     * @return A component whose paint() method will render the specified value
     * (this component).
     * 
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent
     */
    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {
        if (value != null) {
            Enum<?> val = (Enum<?>) value;
            String key;
            if (messagePrefix == null) {
                key = val.toString().toLowerCase();
            } else {
                key = messagePrefix + val.toString().toLowerCase();
            }
            value = messages.getString(key);
        }

        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }

}
