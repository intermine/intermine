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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang.StringUtils;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.text.DecimalDocument;
import org.intermine.common.swing.text.IntegerDocument;
import org.intermine.common.swing.text.RestrictedInputDocument;
import org.intermine.install.project.source.ObjectFactory;
import org.intermine.install.project.source.PropertyDescriptor;
import org.intermine.install.project.source.PropertyType;
import org.intermine.install.swing.ProjectEditor;

/**
 * Class for creating <code>PropertyComponentWrapper</code> objects holding the components
 * displayed in <code>SourcePropertiesPanel</code>. Creates the correct components for the
 * type of property being displayed.
 */
public class PropertyComponentCreator
{
    /**
     * Allowable characters for class fields.
     */
    private static final String CLASS_CHARACTERS = RestrictedInputDocument.WORD_CHARACTERS + ".$";
    
    /**
     * Create a wrapper and components for the given property.
     *  
     * @param propertyName The name of the property.
     * @param descriptor The descriptor of the property. If <code>null</code>, a default
     * descriptor will be created for the property.
     * @param initialValue The initial value for the field.
     * 
     * @return The wrapper around the display components.
     */
    public static PropertyComponentWrapper createComponentFor(
            String propertyName,
            PropertyDescriptor descriptor,
            String initialValue) {
        
        PropertyComponentWrapper wrapper =
            createComponent(propertyName, descriptor, initialValue);
        JComponent component = wrapper.getValueComponent();
        descriptor = wrapper.getDescriptor();
        assert descriptor != null;
        
        if (descriptor.isRequired()) {
            switch (descriptor.getType()) {
                case STRING:
                case INTEGER:
                case DECIMAL:
                case CLASS:
                case FILE:
                case DIRECTORY:
                    JTextComponent textComp = (JTextComponent) component;
                    new RequiredPropertyFocusListener(textComp);
                    break;
            }
        }
        return wrapper;
    }
    
    /**
     * Create a wrapper and components for the given property.
     *  
     * @param propertyName The name of the property.
     * @param descriptor The descriptor of the property. If <code>null</code>, a default
     * descriptor will be created for the property.
     * @param initialValue The initial value for the field.
     * 
     * @return The wrapper around the display components.
     */
    protected static PropertyComponentWrapper createComponent(
            String propertyName,
            PropertyDescriptor descriptor,
            String initialValue) {

        if (descriptor == null) {
            ObjectFactory factory = new ObjectFactory();
            descriptor = factory.createPropertyDescriptor();
            descriptor.setName(propertyName);
            descriptor.setRequired(false);
            descriptor.setType(PropertyType.STRING);
            descriptor.setHidden(false);
        }
        
        JCheckBox checkBox;
        JTextField textField;
        switch (descriptor.getType()) {
            case BOOLEAN:
                checkBox = new JCheckBox();
                checkBox.setSelected(Boolean.parseBoolean(initialValue));
                return new PropertyComponentWrapper(propertyName, descriptor, checkBox);
                
            case INTEGER:
                textField = new JTextField(new IntegerDocument(), initialValue, 0);
                return new PropertyComponentWrapper(propertyName, descriptor, textField);
                
            case DECIMAL:
                textField = new JTextField(new DecimalDocument(), initialValue, 0);
                return new PropertyComponentWrapper(propertyName, descriptor, textField);
                
            case CLASS:
                textField =
                    new JTextField(new RestrictedInputDocument(CLASS_CHARACTERS), initialValue, 0);
                return new PropertyComponentWrapper(propertyName, descriptor, textField);
                
            case STRING:
                textField = new JTextField(initialValue);
                if (descriptor != null) {
                    if (descriptor.getValidation() != null) {
                            new ValidatedPropertyFocusListener(
                                    textField, descriptor.getValidation(),
                                    descriptor.isRequired());
                    }
                }
                return new PropertyComponentWrapper(propertyName, descriptor, textField);
                
            case DIRECTORY:
            case FILE:
                return createFileComponent(propertyName, descriptor, initialValue);
        }
        throw new IllegalArgumentException("Don't know how to create a component for field type "
                                           + descriptor.getType());
    }
    
    /**
     * Creates the wrapper and components for file and directory properties.
     * This is a panel containing a text component and a button to open a file
     * browser.
     * 
     * @param propertyName The name of the property.
     * @param descriptor The descriptor of the property.
     * @param initialValue The initial value for the field.
     * 
     * @return The wrapper around the display components.
     */
    protected static PropertyComponentWrapper createFileComponent(
            String propertyName,
            PropertyDescriptor descriptor,
            String initialValue) {

        switch (descriptor.getType()) {
            case FILE:
            case DIRECTORY:
                // ok.
                break;
                
            default:
                throw new IllegalArgumentException(
                        "Only FILE and DIRECTORY types can be handled by createFileComponent");
        }
        
        File initialFile = null;
        if (initialValue != null) {
            initialFile = new File(initialValue);
        }
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        JTextField text = new JTextField(initialValue);
        panel.add(text, BorderLayout.CENTER);
        panel.add(new JButton(new FileComponentBrowseAction(text, descriptor, initialFile)),
                  BorderLayout.EAST);
        
        return new PropertyComponentWrapper(propertyName, descriptor, text, panel);
    }

    /**
     * Listener for text components that have a validation element to check whether the
     * content of the field matches the regular expression it needs to satisfy.
     */
    private static class ValidatedPropertyFocusListener implements FocusListener
    {
        /**
         * The text component to watch.
         */
        private JTextComponent component;
        
        /**
         * The pattern the field needs to satisfy.
         */
        private Pattern pattern;
        
        /**
         * Flag indicating that the field is required. If unset, empty values are
         * acceptable.
         */
        private boolean required;
        
        /**
         * The regular background colour for when the field's value is good.
         */
        private Color normal;
        
        
        /**
         * Initialise to listen for focus changes on the given text component.
         * <p>Adds this listener to <code>comp</code> as a focus listener.</p>
         * 
         * @param comp The text component to watch.
         * @param regex The pattern the field needs to satisfy.
         * @param required Flag indicating that the field is required. If unset,
         * empty values are acceptable.
         */
        public ValidatedPropertyFocusListener(JTextComponent comp,
                                              String regex,
                                              boolean required) {
            component = comp;
            pattern = Pattern.compile(regex);
            normal = component.getBackground();
            this.required = required;
            component.addFocusListener(this);
        }
        
        /**
         * Called when the component gains focus. Resets the background to the regular
         * colour.
         * 
         * @param event The focus event.
         */
        @Override
        public void focusGained(FocusEvent event) {
            component.setBackground(normal);
        }

        /**
         * Called when the component loses focus. Checks the value of the field against
         * the pattern and sets the background colour according to whether the value is
         * valid.
         * 
         * @param event The focus event.
         */
        @Override
        public void focusLost(FocusEvent event) {
            String text = component.getText();
            if (StringUtils.isNotEmpty(text)) {
                if (!pattern.matcher(text).matches()) {
                    component.setBackground(ProjectEditor.ERROR_FIELD_COLOR);
                }
            } else {
                if (required) {
                    component.setBackground(ProjectEditor.ERROR_FIELD_COLOR);
                }
            }
        }
    }
    
    /**
     * Focus listener for required fields. Changes the field's background colour
     * if no value is set.
     */
    private static class RequiredPropertyFocusListener implements FocusListener
    {
        /**
         * The text component to watch.
         */
        private JTextComponent component;
        
        /**
         * The regular background colour for when the field's value is good.
         */
        private Color normal;
        
        /**
         * Initialise to listen for focus changes on the given text component.
         * <p>Adds this listener to <code>comp</code> as a focus listener.</p>
         * 
         * @param comp The text component to watch.
         */
        public RequiredPropertyFocusListener(JTextComponent comp) {
            component = comp;
            normal = component.getBackground();
            component.addFocusListener(this);
        }
        
        /**
         * Called when the component gains focus. Resets the background to the regular
         * colour.
         * 
         * @param event The focus event.
         */
        @Override
        public void focusGained(FocusEvent event) {
            component.setBackground(normal);
        }

        /**
         * Called when the component loses focus. Checks the value of the field and sets
         * the background colour according to whether there is a value.
         * 
         * @param event The focus event.
         */
        @Override
        public void focusLost(FocusEvent event) {
            String text = component.getText();
            if (StringUtils.isNotEmpty(text)) {
                component.setBackground(normal);
            } else {
                component.setBackground(ProjectEditor.ERROR_FIELD_COLOR);
            }
        }
    }
    
    /**
     * Action to display a file chooser dialog when a file or directory property
     * has its "browse" button clicked.
     */
    private static class FileComponentBrowseAction extends AbstractAction
    {
        private static final long serialVersionUID = 1968827713088082955L;

        /**
         * The text field that displays the selected path.
         * @serial
         */
        private JTextField textField;
        
        /**
         * The file chooser dialog.
         * @serial
         */
        private JFileChooser fileChooser;
        
        /**
         * Initialise to set the given text field from files selected from the file chooser.
         * 
         * @param textField The text field to update.
         * @param descriptor The descriptor of the property.
         * @param initialFile The initial file for file chooser.
         */
        public FileComponentBrowseAction(
                JTextField textField,
                PropertyDescriptor descriptor,
                File initialFile) {

            super(Messages.getMessage("browse"));
            this.textField = textField;
            boolean directory = descriptor.getType() == PropertyType.DIRECTORY;

            fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setAcceptAllFileFilterUsed(!directory);
            if (directory) {
                FileFilter filter = new DirectoryFileFilter();
                fileChooser.addChoosableFileFilter(filter);
                fileChooser.setFileFilter(filter);
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            } else {
                String extension = descriptor.getExtension();
                if (extension != null) {
                    FileFilter filter = new FileExtensionFilter(extension);
                    fileChooser.addChoosableFileFilter(filter);
                    fileChooser.setFileFilter(filter);
                }
            }
            if (initialFile != null) {
                fileChooser.setSelectedFile(initialFile);
            }
        }

        /**
         * Called when the "browse" button is fired, displays the file chooser and should
         * a file or directory be selected, sets the text field to the path of the selected
         * file.
         * 
         * @param event The action event.
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            int choice = fileChooser.showOpenDialog(textField);
            if (choice == JFileChooser.APPROVE_OPTION) {
                File f = fileChooser.getSelectedFile();
                String path = f.getAbsolutePath();
                textField.setText(path);
            }
        }
    }
    
    /**
     * A file filter for directories in a JFileChooser.
     */
    private static class DirectoryFileFilter extends FileFilter
    {
        /**
         * Check whether the given file is a directory.
         * 
         * @param f The file to check.
         * 
         * @return <code>true</code> if <code>f</code> is a directory,
         * <code>false</code> if not.
         */
        @Override
        public boolean accept(File f) {
            return f.isDirectory();
        }

        /**
         * Get the description of the filter for display.
         * 
         * @return The text to display.
         */
        @Override
        public String getDescription() {
            return Messages.getMessage("filefilter.directory");
        }
    }
    /**
     * A file filter for files with an extension in a JFileChooser.
     */
    private static class FileExtensionFilter extends FileFilter
    {
        private String extension;

        public FileExtensionFilter(String ext) {
            extension = ext;
        }
        /**
         * Check whether the given file has the appropriate extension.
         * 
         * @param f The file to check.
         * 
         * @return <code>true</code> if <code>f</code> is a directory,
         * or has the appropriate extension, <code>false</code> if not.
         */
        @Override
        public boolean accept(File f) {
            boolean ok = f.isDirectory();
            if (! ok) {
                ok = f.getName().toUpperCase().endsWith(extension.toUpperCase());
            }   
            return ok;
        }

        /**
         * Get the description of the filter for display.
         * 
         * @return The text to display.
         */
        @Override
        public String getDescription() {
            String message;

            if (Messages.hasMessage("filefilter.extension." + extension)) {
                message = "filefilter.extension." + extension;
            } else {
                message = extension + Messages.getMessage("filefilter.files");
            }
            return Messages.getMessage(message);
        }
    }
}
