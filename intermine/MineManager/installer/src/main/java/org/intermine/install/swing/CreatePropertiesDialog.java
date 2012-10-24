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
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.validator.EmailValidator;
import org.intermine.common.swing.ButtonPanel;
import org.intermine.common.swing.GridBagHelper;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.StandardJDialog;
import org.intermine.common.swing.text.LimitedSizeDocument;
import org.intermine.common.swing.text.RestrictedInputDocument;
import org.intermine.install.properties.InterminePropertyKeys;
import org.intermine.install.properties.MinePropertiesLoader;


/**
 * Dialog for setting the properties in the mine's user properties file.
 */
public class CreatePropertiesDialog extends StandardJDialog
{
    private static final long serialVersionUID = 7869500810276588066L;

    /**
     * Acceptable characters for the user name fields.
     * @see RestrictedInputDocument
     */
    private static final String USERNAME_CHARACTERS = RestrictedInputDocument.WORD_CHARACTERS + "-";
    
    /**
     * Acceptable characters for the server name fields.
     * @see RestrictedInputDocument
     */
    private static final String HOST_CHARACTERS = RestrictedInputDocument.WORD_CHARACTERS + "-.";

    /**
     * Acceptable characters for the email fields.
     * @see RestrictedInputDocument
     */
    private static final String EMAIL_CHARACTERS = RestrictedInputDocument.WORD_CHARACTERS + "-.@";
    
    /**
     * The make mine dialog that follows this dialog in the mine set up chain.
     * @serial
     */
    private MakeMineDialog makeMineDialog;
    
    /**
     * Action to proceed to the next step.
     * @serial
     */
    private Action nextAction = new NextAction();
    
    
    /**
     * Text field for the deployment base URL.
     * @serial
     */
    private JTextField webDeployUrlField = new JTextField(40);
    
    /**
     * Text field for the web path.
     * @serial
     */
    private JTextField webPathField = new JTextField(20);
    
    /**
     * Text field for the web application manager user name.
     * @serial
     */
    private JTextField webManagerField =
        new JTextField(new RestrictedInputDocument(USERNAME_CHARACTERS), "", 20);
    
    /**
     * Text field for the web application manager password.
     * @serial
     */
    private JTextField webPasswordField = new JTextField(20);
    
    /**
     * Text field for the web application base URL.
     * @serial
     */
    private JTextField webBaseUrlField = new JTextField(40);
    
    
    /**
     * Text field for the web application super user name.
     * @serial
     */
    private JTextField superuserField =
        new JTextField(new RestrictedInputDocument(EMAIL_CHARACTERS), "", 20);
    
    /**
     * Text field for the web application super user password.
     * @serial
     */
    private JTextField superuserPasswordField = new JTextField(20);
    

    /**
     * Text field for the outgoing email server.
     * @serial
     */
    private JTextField mailHostField =
        new JTextField(new RestrictedInputDocument(HOST_CHARACTERS), "", 30);

    /**
     * Text field for the outgoing email from address.
     * @serial
     */
    private JTextField mailFromField =
        new JTextField(new RestrictedInputDocument(EMAIL_CHARACTERS), "", 30);
    
    /**
     * Text field for the outgoing email subject.
     * @serial
     */
    private JTextField mailSubjectField = new JTextField(new LimitedSizeDocument(128), "", 40);

    /**
     * Text field for the outgoing email body.
     * @serial
     */
    private JTextField mailTextField = new JTextField(40);
    

    /**
     * Text field for the project title.
     * @serial
     */
    private JTextField titleField = new JTextField(40);

    /**
     * Text field for the project subtitle.
     * @serial
     */
    private JTextField subtitleField = new JTextField(40);

    /**
     * Text field for the release version.
     * @serial
     */
    private JTextField versionField = new JTextField(10);

    /**
     * Text field for the site prefix.
     * @serial
     */
    private JTextField prefixField = new JTextField(40);

    /**
     * Text field for the help URL.
     * @serial
     */
    private JTextField helpField = new JTextField(50);

    /**
     * Text field for the feedback email address.
     * @serial
     */
    private JTextField feedbackField =
        new JTextField(new RestrictedInputDocument(EMAIL_CHARACTERS), "", 30);

    /**
     * Check box for the verbose query log.
     * @serial
     */
    private JCheckBox verboseLogCheck =
        new JCheckBox(Messages.getMessage("properties.misc.verboselog"));
    
    /**
     * Background colour for text fields in a normal state.
     * @serial
     */
    private Color standardBackground;
    
    /**
     * Background colour for text fields in error.
     * @serial
     */
    private Color errorBackground;
    
    
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
    public CreatePropertiesDialog(Dialog owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Frame.
     * @param owner The parent Frame.
     */
    public CreatePropertiesDialog(Frame owner) {
        super(owner);
        init();
    }

    /**
     * Initialise with a parent Window.
     * @param owner The parent Window.
     */
    public CreatePropertiesDialog(Window owner) {
        super(owner);
        init();
    }

    /**
     * Common initialisation: lays out the child components and wires up the necessary
     * event listeners. 
     */
    private void init() {
        setName("Create Properties Dialog");
        setTitle(Messages.getMessage("mineinfo.title"));
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        
        Container cp = getContentPane();
        GridBagConstraints cons = GridBagHelper.setup(cp);
        
        cons.weightx = 1;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new JLabel(Messages.getMessage("properties.project.header")), cons);
        
        cons.weightx = 0;
        cons.gridwidth = 1;
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.project.title")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.project.subtitle")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.project.releaseversion")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.project.siteprefix")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.project.helplocation")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.project.feedback")), cons);
        
        cons.gridy++;
        cons.weightx = 1;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new JLabel(Messages.getMessage("properties.webapp.header")), cons);
        
        cons.gridy++;
        cons.weightx = 0;
        cons.gridwidth = 1;
        cp.add(new JLabel(Messages.getMessage("properties.webapp.deploy")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.webapp.path")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.webapp.baseurl")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.webapp.manager")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.webapp.manager.password")), cons);
        
        cons.gridy++;
        cons.weightx = 1;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new JLabel(Messages.getMessage("properties.mail.header")), cons);
        
        cons.gridy++;
        cons.weightx = 0;
        cons.gridwidth = 1;
        cp.add(new JLabel(Messages.getMessage("properties.mail.host")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.mail.from")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.mail.subject")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.mail.text")), cons);
        
        cons.gridy++;
        cons.weightx = 1;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new JLabel(Messages.getMessage("properties.superuser.header")), cons);
        
        cons.gridy++;
        cons.weightx = 0;
        cons.gridwidth = 1;
        cp.add(new JLabel(Messages.getMessage("properties.superuser")), cons);
        
        cons.gridy++;
        cp.add(new JLabel(Messages.getMessage("properties.superuser.password")), cons);
        
        cons.gridy++;
        cons.weightx = 1;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cp.add(new JLabel(Messages.getMessage("properties.misc.header")), cons);
        
        cons.gridy++;
        cp.add(verboseLogCheck, cons);
        
        // Record the next free row for the button panel.
        int rowIndex = cons.gridy + 1;
        
        cons.gridx++;
        cons.gridy = 1;
        cons.weightx = 1;
        cons.gridwidth = 1;
        cp.add(titleField, cons);

        cons.gridy++;
        cp.add(subtitleField, cons);

        cons.gridy++;
        cp.add(versionField, cons);

        cons.gridy++;
        cp.add(prefixField, cons);

        cons.gridy++;
        cp.add(helpField, cons);

        cons.gridy++;
        cp.add(feedbackField, cons);

        cons.gridy += 2;
        cp.add(webDeployUrlField, cons);

        cons.gridy++;
        cp.add(webPathField, cons);

        cons.gridy++;
        cp.add(webBaseUrlField, cons);

        cons.gridy++;
        cp.add(webManagerField, cons);

        cons.gridy++;
        cp.add(webPasswordField, cons);

        cons.gridy += 2;
        cp.add(mailHostField, cons);

        cons.gridy++;
        cp.add(mailFromField, cons);

        cons.gridy++;
        cp.add(mailSubjectField, cons);

        cons.gridy++;
        cp.add(mailTextField, cons);

        cons.gridy += 2;
        cp.add(superuserField, cons);

        cons.gridy++;
        cp.add(superuserPasswordField, cons);

        cons.gridy = rowIndex;
        cons.gridx = 0;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.weightx = 1.0;
        cp.add(new ButtonPanel(getRootPane(), 0, nextAction, new CancelAction()), cons);

        standardBackground = webBaseUrlField.getBackground();
        errorBackground = new Color(255, 232, 232);
        
        verboseLogCheck.setToolTipText(Messages.getMessage("properties.misc.verboselog.tooltip"));
        
        new EmailFieldListener(superuserField, true);
        new EmailFieldListener(mailFromField, true);
        new EmailFieldListener(feedbackField, true);
        
        new UrlFieldListener(webDeployUrlField, false);
        new UrlFieldListener(webBaseUrlField, false);
        new UrlFieldListener(prefixField, false);
        new UrlFieldListener(helpField, true);
        
        pack();
    }
    
    /**
     * Set the "make mine" dialog that will follow this dialog in
     * the mine creation chain.
     * 
     * @param makeMineDialog The MakeMineDialog.
     */
    public void setMakeMineDialog(MakeMineDialog makeMineDialog) {
        this.makeMineDialog = makeMineDialog;
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
        
        previousProperties = props;
        
        String mineName = props.getProperty(InterminePropertyKeys.MINE_NAME);
        assert mineName != null;
        
        if (!mineName.equals(lastMineName)) {
            
            String lmine = mineName.toLowerCase();

            webDeployUrlField.setText(props.getProperty(InterminePropertyKeys.WEBAPP_DEPLOY_URL));
            webPathField.setText(props.getProperty(InterminePropertyKeys.WEBAPP_PATH, lmine));
            webManagerField.setText(props.getProperty(InterminePropertyKeys.WEBAPP_MANAGER));
            webPasswordField.setText(props.getProperty(InterminePropertyKeys.WEBAPP_PASSWORD));
            webBaseUrlField.setText(props.getProperty(InterminePropertyKeys.WEBAPP_BASE_URL));
            
            superuserField.setText(props.getProperty(InterminePropertyKeys.SUPERUSER_ACCOUNT));
            superuserPasswordField.setText(
                    props.getProperty(InterminePropertyKeys.SUPERUSER_PASSWORD));
            
            mailHostField.setText(props.getProperty(InterminePropertyKeys.MAIL_HOST));
            mailFromField.setText(props.getProperty(InterminePropertyKeys.MAIL_FROM));
            mailSubjectField.setText(props.getProperty(InterminePropertyKeys.MAIL_SUBJECT));
            mailTextField.setText(props.getProperty(InterminePropertyKeys.MAIL_TEXT));
            
            titleField.setText(props.getProperty(InterminePropertyKeys.PROJECT_TITLE, mineName));
            subtitleField.setText(props.getProperty(InterminePropertyKeys.PROJECT_SUBTITLE));
            versionField.setText(props.getProperty(InterminePropertyKeys.PROJECT_VERSION));
            prefixField.setText(props.getProperty(InterminePropertyKeys.PROJECT_PREFIX));
            helpField.setText(props.getProperty(InterminePropertyKeys.PROJECT_HELP));
            feedbackField.setText(props.getProperty(InterminePropertyKeys.FEEDBACK));
            
            verboseLogCheck.setSelected(
                    Boolean.valueOf(props.getProperty(InterminePropertyKeys.VERBOSE_LOG)));
            
            lastMineName = mineName;
        }
        
        setLocation(location);
    }
    
    /**
     * Update the enabled state of the "next" action according to whether the
     * fields of this dialog are complete and valid.
     * 
     * @param ok A previously calculated state for the action.
     */
    protected void updateState(boolean ok) {
        ok = ok && titleField.getText().length() > 0;
        ok = ok && prefixField.getText().length() > 0;
        ok = ok && helpField.getText().length() > 0;
        
        ok = ok && webDeployUrlField.getText().length() > 0;
        ok = ok && webPathField.getText().length() > 0;
        ok = ok && webBaseUrlField.getText().length() > 0;
        
        nextAction.setEnabled(ok);
    }
    
    /**
     * Specific means of displaying an error message if saving the properties fails.
     * 
     * @param error The exception.
     */
    protected void showExceptionDialog(Exception error) {
        String displayMessage;
        if (error.getCause() != null) {
            logger.error("Failed to write project properties:", error.getCause());
            StringWriter swriter = new StringWriter();
            PrintWriter writer = new PrintWriter(swriter);
            error.getCause().printStackTrace(writer);
            writer.close();
            
            displayMessage =
                Messages.getMessage("properties.write.fail.message", swriter.toString());
        } else {
            logger.error("Failed to write project properties:", error);
            displayMessage =
                Messages.getMessage("properties.write.fail.message", error.getMessage());
        }
        
        JOptionPane.showMessageDialog(CreatePropertiesDialog.this,
                                      displayMessage,
                                      Messages.getMessage("properties.write.fail.title"),
                                      JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Set the properties for the mine set up properties from the fields of the dialog.
     * 
     * @param props The properties object to populate.
     */
    protected void setProperties(Properties props) {
        props.put(InterminePropertyKeys.WEBAPP_DEPLOY_URL, webDeployUrlField.getText());
        props.put(InterminePropertyKeys.WEBAPP_PATH, webPathField.getText());
        props.put(InterminePropertyKeys.WEBAPP_MANAGER, webManagerField.getText());
        props.put(InterminePropertyKeys.WEBAPP_PASSWORD, webPasswordField.getText());
        props.put(InterminePropertyKeys.WEBAPP_BASE_URL, webBaseUrlField.getText());

        props.put(InterminePropertyKeys.SUPERUSER_ACCOUNT, superuserField.getText());
        props.put(InterminePropertyKeys.SUPERUSER_PASSWORD, superuserPasswordField.getText());

        props.put(InterminePropertyKeys.MAIL_HOST, mailHostField.getText());
        props.put(InterminePropertyKeys.MAIL_FROM, mailFromField.getText());
        props.put(InterminePropertyKeys.MAIL_SUBJECT, mailSubjectField.getText());
        props.put(InterminePropertyKeys.MAIL_TEXT, mailTextField.getText());

        props.put(InterminePropertyKeys.PROJECT_TITLE, titleField.getText());
        props.put(InterminePropertyKeys.PROJECT_SUBTITLE, subtitleField.getText());
        props.put(InterminePropertyKeys.PROJECT_VERSION, versionField.getText());
        props.put(InterminePropertyKeys.PROJECT_PREFIX, prefixField.getText());
        props.put(InterminePropertyKeys.PROJECT_HELP, helpField.getText());
        props.put(InterminePropertyKeys.FEEDBACK, feedbackField.getText());
        
        props.put(InterminePropertyKeys.VERBOSE_LOG,
                Boolean.toString(verboseLogCheck.isSelected()));
        props.put(InterminePropertyKeys.STANDALONE_PROJECT, "true"); //should always be true
    }
    
    /**
     * Write the properties in this dialog's field to the file on disk.
     * 
     * @return <code>true</code> if the file was written, <code>false</code> if not.
     */
    protected boolean writeProperties() {
        
        try {
            String mineName = previousProperties.getProperty(InterminePropertyKeys.MINE_NAME);
            MinePropertiesLoader.saveProperties(mineName, previousProperties);
            return true;
        } catch (IOException e) {
            showExceptionDialog(e);
        }
        
        return false;
    }
    
    /**
     * Action to move to the next dialog in the mine creation chain.
     * The action uses the values from the dialog's fields to populate the
     * mine's user properties file.
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
         * Called to write the properties to disk, and if successful, move on to
         * the make mine dialog.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            Cursor current = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                
            try {
                setProperties(previousProperties);
                boolean ok = writeProperties();
                
                if (ok) {
                    makeMineDialog.open(previousProperties, getLocation());
                    setVisible(false);
                    makeMineDialog.setVisible(true);
                }
            } finally {
                setCursor(current);
            }
        }
    }
    
    /**
     * Document listener for email text fields to ensure the field's content is
     * a correctly formatted email address.
     */
    private class EmailFieldListener implements DocumentListener
    {
        /**
         * Email validator.
         */
        private EmailValidator validator = EmailValidator.getInstance();
        
        /**
         * The text component being monitored.
         */
        private JTextComponent component;
        
        /**
         * Flag indicating whether an empty field is acceptable.
         */
        private boolean emptyOk;
        
        /**
         * Initialise to watch the given component. Adds itself as a document
         * listener to <code>comp</code>. 
         * 
         * @param comp The text component to watch.
         * @param emptyOk Whether an empty field is acceptable.
         */
        public EmailFieldListener(JTextComponent comp, boolean emptyOk) {
            component = comp;
            this.emptyOk = emptyOk;
            component.getDocument().addDocumentListener(this);
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
         * Check whether the field contains a valid email. Updates
         * the "next" action as appropriate.
         * 
         * @see CreatePropertiesDialog#updateState(boolean)
         */
        private void update() {
            String text = component.getText();
            boolean ok;
            if (emptyOk && text.length() == 0) {
                ok = true;
            } else {
                ok = validator.isValid(text);
                if (ok) {
                    component.setBackground(standardBackground);
                    component.setToolTipText(null);
                } else {
                    component.setBackground(errorBackground);
                    component.setToolTipText(Messages.getMessage("field.error.email"));
                }
            }
            updateState(ok);
        }
    }
    
    /**
     * Document listener for URL text fields to ensure the field's content is
     * a correctly formatted URL.
     */
    private class UrlFieldListener implements DocumentListener
    {
        // UrlValidator doesn't seem to work as desired.
        //private UrlValidator validator = new UrlValidator();
        
        /**
         * The text component being monitored.
         */
        private JTextComponent component;
        
        /**
         * Flag indicating whether an empty field is acceptable.
         */
        private boolean emptyOk;
        

        /**
         * Initialise to watch the given component. Adds itself as a document
         * listener to <code>comp</code>. 
         * 
         * @param comp The text component to watch.
         * @param emptyOk Whether an empty field is acceptable.
         */
        public UrlFieldListener(JTextComponent comp, boolean emptyOk) {
            component = comp;
            this.emptyOk = emptyOk;
            component.getDocument().addDocumentListener(this);
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
         * Check whether the field contains a valid URL. Updates
         * the "next" action as appropriate.
         * 
         * @see CreatePropertiesDialog#updateState(boolean)
         */
        private void update() {
            String text = component.getText();
            boolean ok;
            if (emptyOk && text.length() == 0) {
                ok = true;
            } else {
                // This, see, doesn't seem to work properly.
                //ok = validator.isValid(text);
                
                try {
                    new URL(text);
                    ok = true;
                } catch (MalformedURLException e) {
                    ok = false;
                }
                if (ok) {
                    component.setBackground(standardBackground);
                    component.setToolTipText(null);
                } else {
                    component.setBackground(errorBackground);
                    component.setToolTipText(Messages.getMessage("field.error.url"));
                }
            }
            updateState(ok);
        }
    }
}
