package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * holder class representing an entry in protein atlas xml
 *
 * @author Fengyuan Hu
 *
 */
public class ProteinAtlasEntry
{
    private String version;
    private String url;
    private String geneName;
    private Set<String> geneSynonymSet = new LinkedHashSet<String>();
    private String geneId;
    private String geneIdDb;
    private String geneIdDbVersion;
    private String geneIdXrefId;
    private String geneIdXrefDb;
    private TissueExpression tissueExpression = new TissueExpression();
    private SubcellularLocation subcellularLocation = new SubcellularLocation();
    private RnaExpression rnaExpression = new RnaExpression();
    private Antibody antibody = new Antibody();

    /**
     * @return version
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getGeneName() {
        return geneName;
    }

    public void setGeneName(String geneName) {
        this.geneName = geneName;
    }

    public Set<String> getGeneSynonymSet() {
        return geneSynonymSet;
    }

    public void setGeneSynonymSet(Set<String> geneSynonymSet) {
        this.geneSynonymSet = geneSynonymSet;
    }

    public String getGeneId() {
        return geneId;
    }

    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    public String getGeneIdDb() {
        return geneIdDb;
    }

    public void setGeneIdDb(String geneIdDb) {
        this.geneIdDb = geneIdDb;
    }

    public String getGeneIdDbVersion() {
        return geneIdDbVersion;
    }

    public void setGeneIdDbVersion(String geneIdDbVersion) {
        this.geneIdDbVersion = geneIdDbVersion;
    }

    public String getGeneIdXrefId() {
        return geneIdXrefId;
    }

    public void setGeneIdXrefId(String geneIdXrefId) {
        this.geneIdXrefId = geneIdXrefId;
    }

    public String getGeneIdXrefDb() {
        return geneIdXrefDb;
    }

    public void setGeneIdXrefDb(String geneIdXrefDb) {
        this.geneIdXrefDb = geneIdXrefDb;
    }

    public TissueExpression getTissueExpression() {
        return tissueExpression;
    }

    public void setTissueExpression(TissueExpression tissueExpression) {
        this.tissueExpression = tissueExpression;
    }

    public SubcellularLocation getSubcellularLocation() {
        return subcellularLocation;
    }

    public void setSubcellularLocation(SubcellularLocation subcellularLocation) {
        this.subcellularLocation = subcellularLocation;
    }

    public RnaExpression getRnaExpression() {
        return rnaExpression;
    }

    public void setRnaExpression(RnaExpression rnaExpression) {
        this.rnaExpression = rnaExpression;
    }

    public Antibody getAntibody() {
        return antibody;
    }

    public void setAntibody(Antibody antibody) {
        this.antibody = antibody;
    }

    /**
     * constructor
     */
    public ProteinAtlasEntry() {
    }

    /**
     * class representing a TissueExpression entry in a ProteinAtlasEntry entry
     */
    public class TissueExpression
    {
        private String type;
        private String technology;
        private Set<TissueExpressionSummary> summarySet
            = new LinkedHashSet<TissueExpressionSummary>();
        private String verification;
        private String verificationType;
        private Set<TissueExpressionData> dataSet = new LinkedHashSet<TissueExpressionData>();

        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        public String getTechnology() {
            return technology;
        }
        public void setTechnology(String technology) {
            this.technology = technology;
        }
        public String getVerification() {
            return verification;
        }
        public void setVerification(String verification) {
            this.verification = verification;
        }
        public String getVerificationType() {
            return verificationType;
        }
        public void setVerificationType(String verificationType) {
            this.verificationType = verificationType;
        }
        public Set<TissueExpressionData> getDataSet() {
            return dataSet;
        }
        public void setDataSet(Set<TissueExpressionData> dataSet) {
            this.dataSet = dataSet;
        }
        public Set<TissueExpressionSummary> getSummarySet() {
            return summarySet;
        }
        public void setSummarySet(Set<TissueExpressionSummary> summarySet) {
            this.summarySet = summarySet;
        }
    }

    /**
     * class representing a TissueExpressionSummary entry in a TissueExpression entry
     */
    public class TissueExpressionSummary
    {
        private String summary;
        private String summaryType;
        public String getSummary() {
            return summary;
        }
        public void setSummary(String summary) {
            this.summary = summary;
        }
        public String getSummaryType() {
            return summaryType;
        }
        public void setSummaryType(String summaryType) {
            this.summaryType = summaryType;
        }
    }

    /**
     * class representing a TissueExpressionData entry in a TissueExpression entry
     */
    public class TissueExpressionData
    {
        private String tissue;
        private String tissueStatus;
        private String cellType;
        private Set<Level> levelSet = new LinkedHashSet<Level>();

        public String getTissue() {
            return tissue;
        }
        public void setTissue(String tissue) {
            this.tissue = tissue;
        }
        public String getTissueStatus() {
            return tissueStatus;
        }
        public void setTissueStatus(String tissueStatus) {
            this.tissueStatus = tissueStatus;
        }
        public String getCellType() {
            return cellType;
        }
        public void setCellType(String cellType) {
            this.cellType = cellType;
        }
        public Set<Level> getLevelSet() {
            return levelSet;
        }
        public void setLevelSet(Set<Level> levelSet) {
            this.levelSet = levelSet;
        }
    }

    /**
     * class representing a Level entry in a TissueExpressionData entry
     */
    public class Level
    {
        private String level;
        private String type;
        private String count;

        public String getLevel() {
            return level;
        }
        public void setLevel(String level) {
            this.level = level;
        }
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        public String getCount() {
            return count;
        }
        public void setCount(String count) {
            this.count = count;
        }
    }

    /**
     * class representing a SubcellularLocation entry in a ProteinAtlasEntry entry
     */
    public class SubcellularLocation
    {
        private String type;
        private String technology;
        private String summary;
        private String verification;
        private String verificationType;
        private Set<SubcellularLocationData> dataSet = new LinkedHashSet<SubcellularLocationData>();

        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        public String getTechnology() {
            return technology;
        }
        public void setTechnology(String technology) {
            this.technology = technology;
        }
        public String getSummary() {
            return summary;
        }
        public void setSummary(String summary) {
            this.summary = summary;
        }
        public String getVerification() {
            return verification;
        }
        public void setVerification(String verification) {
            this.verification = verification;
        }
        public String getVerificationType() {
            return verificationType;
        }
        public void setVerificationType(String verificationType) {
            this.verificationType = verificationType;
        }
        public Set<SubcellularLocationData> getDataSet() {
            return dataSet;
        }
        public void setDataSet(Set<SubcellularLocationData> dataSet) {
            this.dataSet = dataSet;
        }
    }

    /**
     * class representing a SubcellularLocationData entry in a SubcellularLocation entry
     */
    public class SubcellularLocationData
    {
        private String cellLine;
        private String verification;
        private String verificationType;
        private String level;
        private String levelType;
        private Set<Location> locSet = new LinkedHashSet<Location>();

        public String getCellLine() {
            return cellLine;
        }
        public void setCellLine(String cellLine) {
            this.cellLine = cellLine;
        }
        public String getVerification() {
            return verification;
        }
        public void setVerification(String verification) {
            this.verification = verification;
        }
        public String getVerificationType() {
            return verificationType;
        }
        public void setVerificationType(String verificationType) {
            this.verificationType = verificationType;
        }
        public String getLevel() {
            return level;
        }
        public void setLevel(String level) {
            this.level = level;
        }
        public String getLevelType() {
            return levelType;
        }
        public void setLevelType(String levelType) {
            this.levelType = levelType;
        }
        public Set<Location> getLocSet() {
            return locSet;
        }
        public void setLocSet(Set<Location> locSet) {
            this.locSet = locSet;
        }
    }

    /**
     * class representing a Location entry in a SubcellularLocationData entry
     */
    public class Location
    {
        private String location;
        private String locationStatus;

        public String getLocation() {
            return location;
        }
        public void setLocation(String location) {
            this.location = location;
        }
        public String getLocationStatus() {
            return locationStatus;
        }
        public void setLocationStatus(String locationStatus) {
            this.locationStatus = locationStatus;
        }
    }

    /**
     * class representing a RnaExpression entry in a ProteinAtlasEntry entry
     */
    public class RnaExpression
    {
        private String technology;
        private String summary;
        private String verification;
        private String verificationType;
        private Set<RnaExpressionData> dataSet;

        public String getTechnology() {
            return technology;
        }
        public void setTechnology(String technology) {
            this.technology = technology;
        }
        public String getSummary() {
            return summary;
        }
        public void setSummary(String summary) {
            this.summary = summary;
        }
        public String getVerification() {
            return verification;
        }
        public void setVerification(String verification) {
            this.verification = verification;
        }
        public String getVerificationType() {
            return verificationType;
        }
        public void setVerificationType(String verificationType) {
            this.verificationType = verificationType;
        }
        public Set<RnaExpressionData> getDataSet() {
            return dataSet;
        }
        public void setDataSet(Set<RnaExpressionData> dataSet) {
            this.dataSet = dataSet;
        }
    }

    /**
     * class representing a RnaExpressionData entry in a RnaExpression entry
     */
    public class RnaExpressionData
    {
        private String cellLine;
        private String level;
        private String levelType;
        private String levelfpkm;

        public String getCellLine() {
            return cellLine;
        }
        public void setCellLine(String cellLine) {
            this.cellLine = cellLine;
        }
        public String getLevel() {
            return level;
        }
        public void setLevel(String level) {
            this.level = level;
        }
        public String getLevelType() {
            return levelType;
        }
        public void setLevelType(String levelType) {
            this.levelType = levelType;
        }
        public String getLevelfpkm() {
            return levelfpkm;
        }
        public void setLevelfpkm(String levelfpkm) {
            this.levelfpkm = levelfpkm;
        }
    }

    /**
     * class representing a Antibody entry in a ProteinAtlasEntry entry
     */
    public class Antibody
    {
        private String id;
        private String releaseVersion;
        private String releaseDate;
        private String antigenSequence;
        private TissueExpression tissueExpression = new TissueExpression();;
        private SubcellularLocation subcellularLocation = new SubcellularLocation();;
        private String westernBlotTechnology;
        private String westernBlotVerificationType;
        private String westernBlotVerification;
        private String proteinArrayTechnology;
        private String proteinArrayVerificationType;
        private String proteinArrayVerification;

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getReleaseVersion() {
            return releaseVersion;
        }
        public void setReleaseVersion(String releaseVersion) {
            this.releaseVersion = releaseVersion;
        }
        public String getReleaseDate() {
            return releaseDate;
        }
        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }
        public String getAntigenSequence() {
            return antigenSequence;
        }
        public void setAntigenSequence(String antigenSequence) {
            this.antigenSequence = antigenSequence;
        }
        public TissueExpression getTissueExpression() {
            return tissueExpression;
        }
        public void setTissueExpression(TissueExpression tissueExpression) {
            this.tissueExpression = tissueExpression;
        }
        public SubcellularLocation getSubcellularLocation() {
            return subcellularLocation;
        }
        public void setSubcellularLocation(SubcellularLocation subcellularLocation) {
            this.subcellularLocation = subcellularLocation;
        }
        public String getWesternBlotTechnology() {
            return westernBlotTechnology;
        }
        public void setWesternBlotTechnology(String westernBlotTechnology) {
            this.westernBlotTechnology = westernBlotTechnology;
        }
        public String getWesternBlotVerificationType() {
            return westernBlotVerificationType;
        }
        public void setWesternBlotVerificationType(String westernBlotVerificationType) {
            this.westernBlotVerificationType = westernBlotVerificationType;
        }
        public String getWesternBlotVerification() {
            return westernBlotVerification;
        }
        public void setWesternBlotVerification(String westernBlotVerification) {
            this.westernBlotVerification = westernBlotVerification;
        }
        public String getProteinArrayTechnology() {
            return proteinArrayTechnology;
        }
        public void setProteinArrayTechnology(String proteinArrayTechnology) {
            this.proteinArrayTechnology = proteinArrayTechnology;
        }
        public String getProteinArrayVerificationType() {
            return proteinArrayVerificationType;
        }
        public void setProteinArrayVerificationType(String proteinArrayVerificationType) {
            this.proteinArrayVerificationType = proteinArrayVerificationType;
        }
        public String getProteinArrayVerification() {
            return proteinArrayVerification;
        }
        public void setProteinArrayVerification(String proteinArrayVerification) {
            this.proteinArrayVerification = proteinArrayVerification;
        }
    }
}
