package org.intermine.web.dataset;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Thomas Riley
 */
public class DataSet
{
    /** Name of the DataSet. */
    public String name;
    /** Name of the tile to insert. */
    public String tileName;
    /** Introduction text. */
    public String introText;
    /** Path to icon image. */
    public String iconImage;
    /** Path to large image. */
    public String largeImage;
    /** List of DataSetSources. */
    public List dataSetSources = new ArrayList();
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List getDataSetSources() {
        return dataSetSources;
    }

    public void addDataSetSource(DataSetSource dataSetSource) {
        this.dataSetSources.add(dataSetSource);
    }
    
    public void addDataSetSource(String sourceName) {
        DataSetSource dss = new DataSetSource();
        dss.setSourceName(sourceName);
        addDataSetSource(dss);
    }

    public String getIconImage() {
        return iconImage;
    }

    public void setIconImage(String iconImage) {
        this.iconImage = iconImage;
    }

    public String getIntroText() {
        return introText;
    }

    public void setIntroText(String introText) {
        this.introText = introText;
    }

    public String getLargeImage() {
        return largeImage;
    }

    public void setLargeImage(String largeImage) {
        this.largeImage = largeImage;
    }

    public String getTileName() {
        return tileName;
    }

    public void setTileName(String tileName) {
        this.tileName = tileName;
    }
}
