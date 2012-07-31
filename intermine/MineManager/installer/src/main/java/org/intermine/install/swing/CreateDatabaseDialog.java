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

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.intermine.common.swing.ButtonPanel;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.StandardJDialog;
import org.intermine.common.swing.text.RestrictedInputDocument;
import org.intermine.install.database.DatabaseConnectionException;
import org.intermine.install.database.DatabaseUtil;
import org.intermine.install.properties.InterminePropertyKeys;
import org.intermine.install.properties.MinePropertiesLoader;


/**
 * Dialog for setting Postgres database settings and creating the
 * databases.
 */
public class CreateDatabaseDialog extends StandardJDialog
{
    private static final long serialVersionUID = 7296970437085613982L;

    /**
     * The properties dialog that follows this dialog in the mine set up chain.
     * @serial
     */
    private CreatePropertiesDialog createPropertiesDialog;
    
    /**
     * Production database server text field.
     * @serial
     */
    private JTextField productionServerField;
    
    /**
     * Production database name text field.
     * @serial
     */
    private JTextField productionNameField;
    
    /**
     * Production database user name text field.
     * @serial
     */
    private JTextField productionUserNameField;
    
    /**
     * Production database password text field.
     * @serial
     */
    private JTextField productionPasswordField;
    
    /**
     * Production database encoding combo box.
     * @serial
     */
    private JComboBox productionEncodingCombo;
    
    /**
     * Common target items database server text field.
     * @serial
     */
    private JTextField itemsServerField;
    
    /**
     * Common target items database name text field.
     * @serial
     */
    private JTextField itemsNameField;
    
    /**
     * Common target items database user name text field.
     * @serial
     */
    private JTextField itemsUserNameField;
    
    /**
     * Common target items database password text field.
     * @serial
     */
    private JTextField itemsPasswordField;

    /**
     * Common target items database encoding combo box.
     * @serial
     */
    private JComboBox itemsEncodingCombo;
    
    /**
     * Profile database server text field.
     * @serial
     */
    private JTextField profileServerField;

    /**
     * Profile database name text field.
     * @serial
     */
    private JTextField profileNameField;

    /**
     * Profile database user name text field.
     * @serial
     */
    private JTextField profileUserNameField;

    /**
     * Profile database password text field.
     * @serial
     */
    private JTextField profilePasswordField;
    
    /**
     * Profile database encoding combo box.
     * @serial
     */
    private JComboBox profileEncodingCombo;
    
    
    /**
     * Action to create the databases.
     * @serial
     */
    private Action createDatabaseAction = new CreateDatabaseAction();
    
    /**
     * Action to proceed to the next step.
     * @serial
     */
    private Action nextAction = new NextAction();
    
    
    /**
     * The mine creation properties so far.
     * @serial
     */
    private Properties previousProperties;
    
    /**
     * The name of the mine this dialog previously dealt with, if any.
     * @serial
     */
    private String lastMineName;
    
    
    /**
     * Initialise with a parent Dialog.
     * @param owner The parent Dialog.
     */
    public CreateDatabaseDialog(Dialog owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Frame.
     * @param owner The parent Frame.
     */
    public CreateDatabaseDialog(Frame owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Window.
     * @param owner The parent Window.
     */
    public CreateDatabaseDialog(Window owner) {
        super(owner);
        init();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {
        setName("Create Database Dialog");
        setTitle(Messages.getMessage("createdatabase.title"));
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        
        final String acceptableCharacters = RestrictedInputDocument.WORD_CHARACTERS + "-";
        String[] encodingOptions = {"SQL_ASCII", "UTF8"};
        
        productionServerField =
            new JTextField(new RestrictedInputDocument(acceptableCharacters), "", 20);
        productionNameField =
            new JTextField(new RestrictedInputDocument(acceptableCharacters), "", 20);
        productionUserNameField =
            new JTextField(new RestrictedInputDocument(acceptableCharacters), "", 20);
        productionPasswordField = new JTextField(20);
        productionEncodingCombo = new JComboBox(encodingOptions);
        
        itemsServerField =
            new JTextField(new RestrictedInputDocument(acceptableCharacters), "", 20);
        itemsNameField =
            new JTextField(new RestrictedInputDocument(acceptableCharacters), "", 20);
        itemsUserNameField =
            new JTextField(new RestrictedInputDocument(acceptableCharacters), "", 20);
        itemsPasswordField = new JTextField(20);
        itemsEncodingCombo = new JComboBox(encodingOptions);
        
        profileServerField =
            new JTextField(new RestrictedInputDocument(acceptableCharacters), "", 20);
        profileNameField =
            new JTextField(new RestrictedInputDocument(acceptableCharacters), "", 20);
        profileUserNameField =
            new JTextField(new RestrictedInputDocument(acceptableCharacters), "", 20);
        profilePasswordField = new JTextField(20);
        profileEncodingCombo = new JComboBox(encodingOptions);
        
        Container cp = getContentPane();
        GridBagConstraints cons = GridBagHelper.setup(cp);
        
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.weightx = 1;
        cp.add(new JLabel(Messages.getMessage("createdatabase.header")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("database.production")), cons);
        
        addDatabaseLabels(cons);
        
        cons.gridy++;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.weightx = 1;
        cp.add(new JLabel(Messages.getMessage("database.commontargetitems")), cons);
        
        addDatabaseLabels(cons);
        
        cons.gridy++;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.weightx = 1;
        cp.add(new JLabel(Messages.getMessage("database.userprofile")), cons);
        
        addDatabaseLabels(cons);
        
        cons.gridx++;
        cons.gridy = 2;
        cons.weightx = 1;
        cp.add(productionServerField, cons);

        cons.gridy++;
        cp.add(productionNameField, cons);

        cons.gridy++;
        cp.add(productionUserNameField, cons);

        cons.gridy++;
        cp.add(productionPasswordField, cons);
        
        cons.gridy++;
        cp.add(productionEncodingCombo, cons);
        
        cons.gridy += 2;
        cp.add(itemsServerField, cons);

        cons.gridy++;
        cp.add(itemsNameField, cons);

        cons.gridy++;
        cp.add(itemsUserNameField, cons);

        cons.gridy++;
        cp.add(itemsPasswordField, cons);
        
        cons.gridy++;
        cp.add(itemsEncodingCombo, cons);
        
        cons.gridy += 2;
        cp.add(profileServerField, cons);

        cons.gridy++;
        cp.add(profileNameField, cons);

        cons.gridy++;
        cp.add(profileUserNameField, cons);

        cons.gridy++;
        cp.add(profilePasswordField, cons);
        
        cons.gridy++;
        cp.add(profileEncodingCombo, cons);
        
        cons.gridy++;
        cons.gridx = 0;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.weightx = 1.0;
        cp.add(new ButtonPanel(getRootPane(), 1, createDatabaseAction,
                               nextAction, new CancelAction()), cons);

        DocumentListener fieldListener = new FieldListener();
        productionServerField.getDocument().addDocumentListener(fieldListener);
        productionNameField.getDocument().addDocumentListener(fieldListener);
        productionUserNameField.getDocument().addDocumentListener(fieldListener);
        productionPasswordField.getDocument().addDocumentListener(fieldListener);
        itemsServerField.getDocument().addDocumentListener(fieldListener);
        itemsNameField.getDocument().addDocumentListener(fieldListener);
        itemsUserNameField.getDocument().addDocumentListener(fieldListener);
        itemsPasswordField.getDocument().addDocumentListener(fieldListener);
        profileServerField.getDocument().addDocumentListener(fieldListener);
        profileNameField.getDocument().addDocumentListener(fieldListener);
        profileUserNameField.getDocument().addDocumentListener(fieldListener);
        profilePasswordField.getDocument().addDocumentListener(fieldListener);
        
        pack();
    }
    
    /**
     * Helper to <code>init</code>, this adds JLabels for each of the five database
     * fields in each block.
     * 
     * @param cons The GridBagConstraints to work from.
     */
    private void addDatabaseLabels(GridBagConstraints cons) {
        Container cp = getContentPane();

        cons.gridwidth = 1;
        cons.weightx = 0;
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("database.server")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("database.name")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("database.username")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("database.password")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("database.encoding")), cons);
    }
    
    /**
     * Set the mine properties dialog that will follow this dialog in
     * the mine creation chain.
     * 
     * @param createPropertiesDialog The CreatePropertiesDialog.
     */
    public void setCreatePropertiesDialog(CreatePropertiesDialog createPropertiesDialog) {
        this.createPropertiesDialog = createPropertiesDialog;
    }

    /**
     * Reset the fields of this dialog to the values from the previous viewing (if the
     * mine name is the same) or the default values, ready for display.
     * <p>This method does not make the dialog visible.</p>
     *  
     * @param props The properties so far for the mine.
     * @param location The position on the screen to position this dialog.
     */
    public void open(Properties props, Point location) {
        
        String mineName = props.getProperty(InterminePropertyKeys.MINE_NAME);
        assert mineName != null;
        
        if (mineName.equals(lastMineName)) {
            
            assert previousProperties != null;
            previousProperties.putAll(props);
            
        } else {
            
            Properties currentProperties = MinePropertiesLoader.readPreviousProperties(mineName);
            currentProperties.putAll(props);
            
            previousProperties = currentProperties;
            
            String lname = mineName.toLowerCase();
            
            String defaultServer = "localhost";
            String defaultProduction = lname;
            String defaultItems = "items-" + lname;
            String defaultProfile = "userprofile-" + lname;
            String defaultUsername = System.getProperty("user.name");
            String defaultPassword = "secret";
            
            productionServerField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.PRODUCTION_SERVER, defaultServer));
            productionNameField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.PRODUCTION_NAME, defaultProduction));
            productionUserNameField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.PRODUCTION_USER_NAME, defaultUsername));
            productionPasswordField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.PRODUCTION_PASSWORD, defaultPassword));
            
            itemsServerField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.ITEMS_SERVER, defaultServer));
            itemsNameField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.ITEMS_NAME, defaultItems));
            itemsUserNameField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.ITEMS_USER_NAME, defaultUsername));
            itemsPasswordField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.ITEMS_PASSWORD, defaultPassword));
            
            profileServerField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.PROFILE_SERVER, defaultServer));
            profileNameField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.PROFILE_NAME, defaultProfile));
            profileUserNameField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.PROFILE_USER_NAME, defaultUsername));
            profilePasswordField.setText(previousProperties.getProperty(
                    InterminePropertyKeys.PROFILE_PASSWORD, defaultPassword));
            
            lastMineName = mineName;
        }
        
        setLocation(location);
    }
    
    /**
     * Update the enabled state of the "create database" and "next" actions
     * according to whether the fields of this dialog are complete. 
     */
    protected void updateState() {
        boolean ok = true;
        
        ok = ok && productionServerField.getText().length() > 0;
        ok = ok && productionNameField.getText().length() > 0;
        ok = ok && productionUserNameField.getText().length() > 0;
        ok = ok && productionPasswordField.getText().length() > 0;
        
        ok = ok && itemsServerField.getText().length() > 0;
        ok = ok && itemsNameField.getText().length() > 0;
        ok = ok && itemsUserNameField.getText().length() > 0;
        ok = ok && itemsPasswordField.getText().length() > 0;
        
        ok = ok && profileServerField.getText().length() > 0;
        ok = ok && profileNameField.getText().length() > 0;
        ok = ok && profileUserNameField.getText().length() > 0;
        ok = ok && profilePasswordField.getText().length() > 0;
        
        createDatabaseAction.setEnabled(ok);
        nextAction.setEnabled(ok);
    }
    
    /**
     * Set the properties for the mine creation from the fields of the dialog.
     * 
     * @param props The properties object to populate.
     */
    protected void setProperties(Properties props) {
        props.put(InterminePropertyKeys.PRODUCTION_SERVER, productionServerField.getText());
        props.put(InterminePropertyKeys.PRODUCTION_NAME, productionNameField.getText());
        props.put(InterminePropertyKeys.PRODUCTION_USER_NAME, productionUserNameField.getText());
        props.put(InterminePropertyKeys.PRODUCTION_PASSWORD, productionPasswordField.getText());
        props.put(InterminePropertyKeys.PRODUCTION_ENCODING,
                        productionEncodingCombo.getSelectedItem());
        
        props.put(InterminePropertyKeys.ITEMS_SERVER, itemsServerField.getText());
        props.put(InterminePropertyKeys.ITEMS_NAME, itemsNameField.getText());
        props.put(InterminePropertyKeys.ITEMS_USER_NAME, itemsUserNameField.getText());
        props.put(InterminePropertyKeys.ITEMS_PASSWORD, itemsPasswordField.getText());
        props.put(InterminePropertyKeys.ITEMS_ENCODING, itemsEncodingCombo.getSelectedItem());
        
        props.put(InterminePropertyKeys.PROFILE_SERVER, profileServerField.getText());
        props.put(InterminePropertyKeys.PROFILE_NAME, profileNameField.getText());
        props.put(InterminePropertyKeys.PROFILE_USER_NAME, profileUserNameField.getText());
        props.put(InterminePropertyKeys.PROFILE_PASSWORD, profilePasswordField.getText());
        props.put(InterminePropertyKeys.PROFILE_ENCODING, profileEncodingCombo.getSelectedItem());
    }
    
    /**
     * Action to start creating the databases. Launches the creation code in 
     * a worker thread.
     */
    private class CreateDatabaseAction extends AbstractAction
    {
        private static final long serialVersionUID = -2321953185268554584L;

        /**
         * Constructor.
         */
        public CreateDatabaseAction() {
            super(Messages.getMessage("createdatabase"));
        }

        /**
         * Called to start database creation.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            Properties props = new Properties();
            setProperties(props);
            
            CreateDatabaseWorker worker =
                new CreateDatabaseWorker(CreateDatabaseDialog.this, props);
            worker.addPropertyChangeListener(new CreateDatabaseListener());
            worker.execute();
        }
    }
    
    /**
     * Property listener for the CreateDatabaseWorker, this class responds
     * to the completed event from the worker and shows a confirmation or
     * error dialog.
     */
    private class CreateDatabaseListener implements PropertyChangeListener
    {
        /**
         * Listener method.
         * 
         * @param event The property change event.
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (CreateDatabaseWorker.COMPLETE.equals(event.getPropertyName())) {
                CreateDatabaseWorker worker = (CreateDatabaseWorker) event.getSource();
                try {
                    Boolean databasesCreated = worker.get();
                    if (databasesCreated) {
                        JOptionPane.showMessageDialog(
                                CreateDatabaseDialog.this,
                                Messages.getMessage("database.created.message"),
                                Messages.getMessage("database.created.title"),
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        showExceptionDialog(worker.getCreationException(),
                                            "database.create.fail.title",
                                            "database.create.fail.message");
                    }
                } catch (CancellationException e) {
                    // Do nothing - the user knows they cancelled it.
                } catch (ExecutionException e) {
                    logger.warn(
                            "ExecutionException getting result from create database worker:", e);
                } catch (InterruptedException e) {
                    // Just leave.
                }
            }
        }
    }
    
    /**
     * Action to move to the next dialog in the mine creation chain.
     * The action uses the values from the dialog's fields to check that
     * the databases exist and can be accessed.
     */
    private class NextAction extends AbstractAction
    {
        private static final long serialVersionUID = 3527854301565949917L;

        /**
         * Constructor.
         */
        public NextAction() {
            super(Messages.getMessage("next"));
        }

        /**
         * Called to check if the databases can be accessed, and if so, move on to
         * the mine properties dialog.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            Cursor current = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                
            try {
                String[] servers = {
                        productionServerField.getText(),
                        itemsServerField.getText(),
                        profileServerField.getText()
                };
                String[] dbNames = {
                        productionNameField.getText(),
                        itemsNameField.getText(),
                        profileNameField.getText()
                };
                String[] userNames = {
                        productionUserNameField.getText(),
                        itemsUserNameField.getText(),
                        profileUserNameField.getText()
                };
                String[] passwords = {
                        productionPasswordField.getText(),
                        itemsPasswordField.getText(),
                        profilePasswordField.getText()
                };
                
                Object[] missingDbs = new String[dbNames.length];
                int missingCount = 0;
                try {
                    for (int i = 0; i < 3; i++) {
                        boolean ok =
                            DatabaseUtil.checkDatabaseExists(
                                    servers[i], dbNames[i], userNames[i], passwords[i]);
                        if (!ok) {
                            missingDbs[missingCount++] = dbNames[i];
                        }
                    }
                    
                    if (missingCount == 0) {
                        setProperties(previousProperties);
                        createPropertiesDialog.open(previousProperties, getLocation());
                        setVisible(false);
                        createPropertiesDialog.setVisible(true);
                        
                    } else {
                        String key = "database.missing." + missingCount;
                        StringBuilder message = new StringBuilder();
                        message.append("<html>");
                        message.append(Messages.getMessage(key, missingDbs));
                        message.append("<br/>");
                        message.append(Messages.getMessage("database.missing.summary"));
                        message.append("</html>");
                        
                        JOptionPane.showMessageDialog(CreateDatabaseDialog.this,
                                                      message,
                                                      Messages.getMessage("database.missing.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                } catch (DatabaseConnectionException e) {
                    showExceptionDialog(e,
                                        "database.test.fail.title",
                                        "database.test.fail.message");
                }
            } finally {
                setCursor(current);
            }
        }
    }
    
    /**
     * Listener for the text fields in the dialog to prevent the "create
     * database" and "next" actions being enabled if the text fields are
     * incomplete.
     * 
     * @see CreateDatabaseDialog#updateState()
     */
    private class FieldListener implements DocumentListener
    {
        @Override
        public void changedUpdate(DocumentEvent e) {
            updateState();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateState();
        }
        
    }
}
