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

import java.awt.GridBagConstraints;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.text.RestrictedInputDocument;
import org.intermine.install.project.event.ProjectListener;
import org.intermine.install.project.event.ProjectListenerSupport;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Source;


/**
 * A panel for displaying information about a project source. Displays the
 * name, type, dump check box and a subpanel containing the properties of
 * the source.
 */
public class SourcePanel extends JPanel
{
    private static final long serialVersionUID = -2958377061385761808L;

    /**
     * The text field for the source name.
     * @serial
     */
    private JTextField nameField =
        new JTextField(new RestrictedInputDocument(
                RestrictedInputDocument.WORD_CHARACTERS + "-+."), "", 24);
    
    /**
     * The read only text field for the source type.
     * @serial
     */
    private JTextField typeField = new JTextField(20);
    
    /**
     * The check box for the source's "dump" flag.
     * @serial
     */
    private JCheckBox dumpCheck = new JCheckBox();
    
    /**
     * The sub panel containing the source's properties components.
     * @serial
     */
    private SourcePropertiesPanel propertiesPanel = new SourcePropertiesPanel();
    
    /**
     * Support for firing <code>ProjectEvent</code>s.
     * @serial
     */
    private ProjectListenerSupport projectListenerSupport = new ProjectListenerSupport(this);
    
    /**
     * The project being edited.
     */
    private transient Project project;
    
    /**
     * The source being edited.
     */
    private transient Source source;
    
    
    /**
     * Constructor.
     */
    public SourcePanel() {
        init();
    }
    
    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {
        typeField.setEditable(false);
        nameField.setEnabled(false);
        typeField.setEnabled(false);
        dumpCheck.setEnabled(false);
        
        GridBagConstraints cons = GridBagHelper.setup(this);
        
        add(new JLabel(Messages.getMessage("source.name")), cons);
        
        cons.gridy++;
        add(new JLabel(Messages.getMessage("source.type")), cons);
        
        cons.gridy++;
        add(new JLabel(Messages.getMessage("source.dump")), cons);
        
        cons.gridx++;
        cons.gridy = 0;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.weightx = 1.0;
        add(nameField, cons);
        
        cons.gridy++;
        add(typeField, cons);
        
        cons.gridy++;
        add(dumpCheck, cons);
        
        cons.gridy++;
        cons.gridx = 0;
        cons.weighty = 1.0;
        cons.fill = GridBagConstraints.BOTH;
        add(new JScrollPane(propertiesPanel), cons);
        
        nameField.setEnabled(false);
        typeField.setEnabled(false);
        dumpCheck.setEnabled(false);
    }
    
    /**
     * Set the source to edit.
     * 
     * @param projectHome The project home directory.
     * @param p The Project.
     * @param s The Source.
     * 
     * @return Whether the source previously edited has been changed.
     */
    public boolean setSource(File projectHome, Project p, Source s) {
        boolean previousChanged = saveCurrentSource();
        
        project = p;
        source = s;
        propertiesPanel.setSource(projectHome, project, source);
        
        boolean hasSource = s != null;
        if (hasSource) {
            nameField.setText(source.getName());
            typeField.setText(source.getType());
            dumpCheck.setSelected(source.isDump() != null && source.isDump());
        } else {
            nameField.setText("");
            typeField.setText("");
            dumpCheck.setSelected(false);
        }
        
        nameField.setEnabled(hasSource);
        typeField.setEnabled(hasSource);
        dumpCheck.setEnabled(hasSource);
        
        revalidate();
        propertiesPanel.repaint();
        
        return previousChanged;
    }
    
    /**
     * Save the values in the fields in this panel and the properties sub panel
     * into the Source object.
     * <p>Fires a <code>ProjectEvent</code> via {@link ProjectListener#sourceModified}.</p>
     * 
     * @return Whether the source previously edited has been changed.
     */
    public boolean saveCurrentSource() {
        boolean changed = false;
        if (source != null) {
            EqualsBuilder equal = new EqualsBuilder();
            equal.append(nameField.getText(), source.getName());
            source.setName(nameField.getText());
            
            boolean sourceDumpValue = Boolean.TRUE.equals(source.isDump());
            equal.append(dumpCheck.isSelected(), sourceDumpValue);
            source.setDump(dumpCheck.isSelected());
            
            boolean changedProperties = propertiesPanel.saveChanges();
            changed = !equal.isEquals() || changedProperties;
        }
        
        if (changed) {
            projectListenerSupport.fireSourceModified(project, source);
        }
        
        return changed;
    }

    /**
     * Add a ProjectListener to this panel.
     * @param l The ProjectListener.
     */
    public void addProjectListener(ProjectListener l) {
        projectListenerSupport.addProjectListener(l);
    }
    
    /**
     * Remove a ProjectListener from this panel.
     * @param l The ProjectListener.
     */
    public void removeProjectListener(ProjectListener l) {
        projectListenerSupport.removeProjectListener(l);
    }
}
