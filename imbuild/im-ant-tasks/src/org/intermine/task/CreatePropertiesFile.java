package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * 
 * 
 * @author Thmoas Riley
 */
public class CreatePropertiesFile extends Task
{
    private String propName;
    private File templateFile;
    private File toFile;
    
    public void setTemplateFile(File file) {
        this.templateFile = file;
    }
    
    public void setToFile(File file) {
        this.toFile = file;
    }
    
    public void execute() throws BuildException {
        if (toFile == null) {
            throw new BuildException("toFile attribute required");
        }
        if (templateFile == null) {
            throw new BuildException("templateFile attribute required");
        }
        
        try {
            if (!toFile.exists()) {
                buildFile();
            }
        } catch (IOException err) {
            throw new BuildException(err);
        }
    }
    
    protected void buildFile() throws IOException {
        BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
        BufferedReader fin = new BufferedReader(new FileReader(templateFile));
        String fline;
        String comments = "";
        StringBuffer results = new StringBuffer();
        String others = "";
        String prompt = null;
        Map properties = new HashMap();
        
        properties.put("ant.project.name", getProject().getName());
        
        System.out.println("#### " + toFile.getName() + " does not exist");
        System.out.println("#### Creating " + toFile.getAbsolutePath());
        System.out.println("#### (Hit Return to accept the default value)");
        
        
        while ((fline = fin.readLine()) != null) {
            fline = fline.trim();
            if (fline.startsWith("#")) {
                if (fline.substring(1).trim().startsWith("[prompt]")) {
                    prompt = fline.substring(10).trim();
                } else {
                    comments += fline + "\n";
                }
            } else if (fline.length() > 0) {
                String parts[] = fline.split("=");
                String var = parts[0].trim();
                String value = parts[1].trim();
                value = expandVars(value, properties);
                if (prompt != null) {
                    System.out.println(" \n" + prompt);
                    System.out.print("(Default is \"" + value + "\")");
                    String input = cin.readLine();
                    if (input.trim().length() > 0) {
                        value = input;
                    }
                    //System.out.println(var + " = \"" + value + "\"");
                } else {
                    others += var + " = " + value + "\n";
                }
                results.append(comments);
                results.append(var + " = " + value + "\n");
                properties.put(var, value);
                prompt = null;
                comments = "";
            } else {
                results.append(comments + "\n");
                comments = "";
            }
        }
        
        FileWriter fw = new FileWriter(toFile);
        fw.write(results.toString());
        fw.close();
        
        System.out.println("" +
                "### Done\n" +
                "### I've also written these default property values:\n" +
                others);
        
    }
    
    protected String expandVars(String value, Map properties) {
        Pattern p = Pattern.compile("\\$\\{([^\\}]+)\\}");
        Matcher m = p.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1);
            if (properties.containsKey(var)) {
                var = (String) properties.get(var);
            } else {
                var = "??" + var + "??";
            }
            m.appendReplacement(sb, var);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
