package org.intermine.install.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Collator;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.StringUtils;
import org.intermine.common.swing.ButtonPanel;
import org.intermine.common.swing.GenericMutableComboBoxModel;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.StandardJDialog;
import org.intermine.common.swing.text.RestrictedInputDocument;
import org.intermine.install.project.event.ProjectListener;
import org.intermine.install.project.event.ProjectListenerSupport;
import org.intermine.install.project.source.PropertyDescriptor;
import org.intermine.install.project.source.SourceInfo;
import org.intermine.install.project.source.SourceInfoLoader;
import org.intermine.modelviewer.project.ObjectFactory;
import org.intermine.modelviewer.project.Project;
import org.intermine.modelviewer.project.Property;
import org.intermine.modelviewer.project.Source;


/**
 * Dialog for adding a data source to a project.
 */
public class AddSourceDialog extends StandardJDialog
{
    private static final long serialVersionUID = 7202500451548893592L;

    /**
     * The permitted characters for the data source name.
     * @see RestrictedInputDocument
     */
    public static final String NAME_CHARACTERS =
        RestrictedInputDocument.LOWER_CASE + RestrictedInputDocument.DIGITS + "-_";
    
    /**
     * The data source name text field.
     * @serial
     */
    private JTextField nameField =
        new JTextField(new RestrictedInputDocument(NAME_CHARACTERS, true), "", 24);
    
    /**
     * The model for the data source type combo box.
     * @serial
     */
    private GenericMutableComboBoxModel<String> typeComboModel =
        new GenericMutableComboBoxModel<String>(Collator.getInstance());
    
    /**
     * The data source type combo box.
     * @serial
     */
    private JComboBox typeCombo = new JComboBox(typeComboModel);

    /**
     * The Action to add the source to the project.
     * @serial
     */
    private Action addAction = new AddAction();
    
    /**
     * The derived type dialog to use when the type selected needs to be
     * derived.
     * @serial
     */
    private NewDerivedTypeDialog newDerivedDialog;
    
    /**
     * Support for firing <code>ProjectListener</code> events.
     * @serial
     */
    private ProjectListenerSupport projectListenerSupport = new ProjectListenerSupport(this);
    
    /**
     * The project being edited.
     */
    private transient Project project;
    
    /**
     * The set of source names already used in the project.
     * @serial
     */
    private Set<String> sourceNames = new HashSet<String>();
    
    
    /**
     * Initialise with a parent Frame.
     * @param owner The parent Frame.
     */
    public AddSourceDialog(Frame owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Dialog.
     * @param owner The parent Dialog.
     */
    public AddSourceDialog(Dialog owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Window.
     * @param owner The parent Window.
     */
    public AddSourceDialog(Window owner) {
        super(owner);
        init();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {
        setName("Add Source Dialog");
        setTitle(Messages.getMessage("source.add.title"));
        
        Container cp = getContentPane();
        
        GridBagConstraints cons = GridBagHelper.setup(cp);
        
        
        cp.add(new JLabel(Messages.getMessage("source.type")), cons);

        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("source.name")), cons);

        cons.gridx++;
        cons.gridy = 0;
        cons.weightx = 1;
        cp.add(typeCombo, cons);

        cons.gridy++;  
        cp.add(nameField, cons);

        cons.gridx = 0;
        cons.gridy++;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new ButtonPanel(addAction, new CancelAction()), cons);
        
        pack();
        
        nameField.getDocument().addDocumentListener(new NameDocumentListener());
        typeCombo.addActionListener(new TypeComboListener());
    }
    
    /**
     * Set the dialog used when a derived type is selected.
     * 
     * @param newDerivedDialog The dialog following this in the creation chain.
     */
    public void setNewDerivedDialog(NewDerivedTypeDialog newDerivedDialog) {
        this.newDerivedDialog = newDerivedDialog;
    }

    /**
     * Initialise this dialog ready for an addition against the given project.
     * 
     * @param project The project to edit.
     */
    public void initialise(Project project) {
        this.project = project;
        
        if (typeComboModel.isEmpty()) {
            typeComboModel.set(SourceInfoLoader.getInstance().getSourceTypes());
        }
        
        sourceNames.clear();
        for (Source s : project.getSources().getSource()) {
            sourceNames.add(s.getName());
        }
        
        clearNameField();
    }
    
    /**
     * Reset the name field text to empty.
     */
    public void clearNameField() {
        nameField.setText("");
    }
    
    /**
      * Update the Name Field when the combo box is selected to give a sane default
      *
      */
    protected void updateName() {
        String type = (String) typeCombo.getSelectedItem();
        nameField.setText(type);
    }
    /**
     * Update the state of the "add" action. It requires a name in the text field
     * that has not already been used and for a type to be selected in the combo box.
     */
    protected void updateState() {
        String name = nameField.getText();
        boolean ok = name.length() > 0;
        ok = ok && !sourceNames.contains(name);
        updateState(ok);
    }
    
    /**
     * Update the state of the "add" action based on the type combo, assuming the name
     * text field has already been checked.
     * 
     * @param ok Whether the name field is ok.
     */
    protected void updateState(boolean ok) {
        ok = ok && typeCombo.getSelectedIndex() >= 0;
        addAction.setEnabled(ok);
    }
    
    /**
     * Add a project listener to this dialog.
     * 
     * @param l The ProjectListener.
     * 
     * @see ProjectListenerSupport#addProjectListener
     */
    public void addProjectListener(ProjectListener l) {
        projectListenerSupport.addProjectListener(l);
    }
    
    /**
     * Remove a project listener from this dialog.
     * 
     * @param l The ProjectListener.
     * 
     * @see ProjectListenerSupport#removeProjectListener
     */
    public void removeProjectListener(ProjectListener l) {
        projectListenerSupport.removeProjectListener(l);
    }
    
    /**
     * The Action to perform when the user is ready to add the data source.
     */
    private class AddAction extends AbstractAction
    {
        private static final long serialVersionUID = -1042891780324233318L;

        /**
         * Constructor.
         */
        public AddAction() {
            super(Messages.getMessage("add"));
            setEnabled(false);
        }

        /**
         * Create the new data Source object and add it to the project registered with
         * the dialog. The source is populated with default values if any are available
         * in the source descriptor. If the data source needs to be derived, control is
         * passed on to the NewDerivedTypeDialog as this dialog closes.
         * 
         * @param event The action event object.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            String name = nameField.getText();
            String type = (String) typeCombo.getSelectedItem();
            assert name.length() > 0 : "No name";
            assert !sourceNames.contains(name) : "Name already used";
            assert type != null : "No type";
            
            SourceInfo sourceInfo =
                SourceInfoLoader.getInstance().getSourceInfo(type);
            if (sourceInfo != null) {
                if (sourceInfo.getSource().getDerivation() != null) {
                    newDerivedDialog.initialise(project, sourceInfo, name);
                    newDerivedDialog.setLocation(getLocation());
                    setVisible(false);
                    newDerivedDialog.setVisible(true);
                    return;
                }
            }
            
            ObjectFactory objFactory = new ObjectFactory();
            
            Source newSource = objFactory.createSource();
            newSource.setName(name);
            newSource.setType(type);
            newSource.setDump(Boolean.FALSE);
            project.getSources().getSource().add(newSource);
            
            sourceInfo = SourceInfoLoader.getInstance().getSourceInfo(type);
            if (sourceInfo != null) {
                for (PropertyDescriptor descriptor : sourceInfo.getSource().getProperty()) {
                    String defaultValue =
                        sourceInfo.getDefaults().getProperty(descriptor.getName());
                    
                    if (defaultValue != null || descriptor.isRequired()) {
                        if (defaultValue == null) {
                            defaultValue = "";
                        }
                        
                        Property p = objFactory.createProperty();
                        p.setName(descriptor.getName());
                        switch (descriptor.getType()) {
                            case FILE:
                            case DIRECTORY:
                                p.setLocation(defaultValue);
                                break;
                                
                            default:
                                p.setValue(defaultValue);
                                break;
                        }
                        newSource.getProperty().add(p);
                    }
                }
            }
            
            projectListenerSupport.fireSourceAdded(project, newSource);
            
            setVisible(false);
        }
    }
    
    /**
     * Document listener for the name field. All changes cause the "add" action to be
     * enabled or disabled as the contents of the field become valid or invalid. Also
     * changes the background colour of the field to reflect this state.
     */
    private class NameDocumentListener implements DocumentListener
    {
        /**
         * The regular background colour.
         */
        private Color normal;
        
        /**
         * Constructor.
         */
        public NameDocumentListener() {
            normal = nameField.getBackground();
        }
        
        @Override
        public void changedUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            update();
        }
        
        /**
         * The update tests used by all forms of change (i.e. all methods from
         * <code>DocumentListener</code>).
         * 
         * @see AddSourceDialog#updateState()
         */
        private void update() {
            String name = nameField.getText();
            boolean ok = false;
            boolean setTooltip = false;
            if (StringUtils.isEmpty(name)) {
                nameField.setBackground(ProjectEditor.ERROR_FIELD_COLOR);
            } else if (sourceNames.contains(name)) {
                nameField.setBackground(ProjectEditor.ERROR_FIELD_COLOR);
                setTooltip = true;
            } else {
                nameField.setBackground(normal);
                ok = true;
            }
            
            if (setTooltip) {
                nameField.setToolTipText(Messages.getMessage("source.duplicatename", name));
            } else {
                nameField.setToolTipText(null);
            }
            
            updateState(ok);
        }
    }
    
    /**
     * Listener to the type combo box. Enables or disables the "add" action
     * according to whether anything is selected.
     */
    private class TypeComboListener implements ActionListener
    {
        /**
         * Update the state of the "add" action as the combo event is received.
         * 
         * @param event The action event object.
         * 
         * @see AddSourceDialog#updateState()
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            updateName();
            updateState();
        }
    }
}
