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

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JRootPane;


/**
 * Simple <code>Box</code> panel that right-justifies a number of Actions as
 * buttons in the panel.
 * 
 * @see Box
 */
public class ButtonPanel extends Box
{

    private static final long serialVersionUID = -7582565038169998908L;

    /**
     * The JButtons created around the actions.
     * @serial
     */
    private JButton[] buttons;
    
    
    /**
     * Creates a new ButtonPanel with a JButton for each of the given actions,
     * assigning the root pane's default button to the button of the given index.
     * 
     * @param rootPane The root pane.
     * @param defaultIndex The index in <code>actions</code> of the default action.
     * @param actions The Actions to use.
     * 
     * @see JRootPane#setDefaultButton(JButton)
     */
    public ButtonPanel(JRootPane rootPane, int defaultIndex, Action... actions) {
        this(actions);
        rootPane.setDefaultButton(buttons[defaultIndex]);
    }
    
    /**
     * Creates a new ButtonPanel with a JButton for each of the given actions.
     * 
     * @param actions The Actions to use.
     */
    public ButtonPanel(Action... actions) {
        super(BoxLayout.X_AXIS);

        buttons = new JButton[actions.length];
        
        add(Box.createHorizontalGlue());
        for (int i = 0; i < actions.length; i++) {
            if (i > 0) {
                add(Box.createHorizontalStrut(8));
            }
            JButton button = new JButton(actions[i]);
            buttons[i] = button;
            add(button);
        }
    }
}
