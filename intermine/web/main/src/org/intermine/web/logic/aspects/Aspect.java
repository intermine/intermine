package org.intermine.web.logic.aspects;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Configuration for a single data set. A data set describes a particular category of data/classes/
 * templates/example queries that are displayed on a 'data set homepage' that acts as a starting
 * point for users interested in a particular aspect of the data.
 * 
 * @author Thomas Riley
 */
public class Aspect
{
    /** Name of the aspect. */
    private String name;
    /** Subtitle. */
    private String subTitle;
    /** Name of the tile to insert. */
    private String tileName;
    /** Introduction text. */
    private String introText;
    /** Path to icon image. */
    private String iconImage;
    /** Path to large image. */
    private String largeImage;
    /** List of AspectSources. */
    private List aspectSources = new ArrayList();
    /** Comma seperated list of starting-point class names. */
    private List startingPoints = new ArrayList();
    
    /**
     * Get the name of the data set.
     * @return the data set name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the data set name.
     * @param name the data set name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the subtitle
     * @return the subtitle
     */
    public String getSubTitle() {
        return subTitle;
    }
    
    /** 
     * Set the subtitle.
     * @param subTitle the subtitle for the data set
     */
    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    /**
     * Get the AspectSources.
     * @return the AspectSources
     * @see org.intermine.web.dataset.AspectSource
     */
    public List getAspectSources() {
        return aspectSources;
    }

    /**
     * Add a AspectSource.
     * @param aspectSource the AspectSource to add
     */
    public void addAspectSource(AspectSource aspectSource) {
        this.aspectSources.add(aspectSource);
    }

    /**
     * Get the icon Image.
     * @return the icon Image
     */
    public String getIconImage() {
        return iconImage;
    }

    /**
     * Set the icon Image
     * @param iconImage the icon Image
     */
    public void setIconImage(String iconImage) {
        this.iconImage = iconImage;
    }

    /**
     * Get the introduction text.
     * @return the introduction text
     */
    public String getIntroText() {
        return introText;
    }

    /**
     * Set the introduction text
     * @param introText the introduction text
     */
    public void setIntroText(String introText) {
        this.introText = introText;
    }

    /**
     * Webapp relative path to large image.
     * @return the path to the large image
     */
    public String getLargeImage() {
        return largeImage;
    }

    /**
     * Set the webapp relative path to the large image.
     * @param largeImage path to large image
     */
    public void setLargeImage(String largeImage) {
        this.largeImage = largeImage;
    }

    /**
     * Get the name of tile/page to embed in the centre of the data set homepage
     * @return the name of the tile/page to embed in the centre of the data set homepage
     */
    public String getTileName() {
        return tileName;
    }

    /**
     * Set the name of the tile/page to embed in the centre of the data set homepage.
     * @param tileName the name of the tile/page to embed in the centre of the data set homepage
     */
    public void setTileName(String tileName) {
        this.tileName = tileName;
    }
    
    /**
     * Set the starting point class names as a space seperated list.
     * @param classnames class names as a space seperated list
     */
    public void setStartingPoints(String classnames) {
        String classes[] = StringUtils.split(classnames);
        classes = StringUtils.stripAll(classes);
        startingPoints = Arrays.asList(classes);
    }
    
    /**
     * Get a List of starting point class names.
     * @return List of class names
     */
    public List getStartingPoints() {
        return startingPoints;
    }
}
