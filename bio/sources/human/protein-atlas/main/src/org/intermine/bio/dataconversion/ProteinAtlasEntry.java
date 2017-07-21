package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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

    /**
     * @param version version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return geneName
     */
    public String getGeneName() {
        return geneName;
    }

    /**
     * @param geneName geneName
     */
    public void setGeneName(String geneName) {
        this.geneName = geneName;
    }

    /**
     * @return geneSynonymSet
     */
    public Set<String> getGeneSynonymSet() {
        return geneSynonymSet;
    }

    /**
     * @param geneSynonymSet geneSynonymSet
     */
    public void setGeneSynonymSet(Set<String> geneSynonymSet) {
        this.geneSynonymSet = geneSynonymSet;
    }

    /**
     * @return geneId
     */
    public String getGeneId() {
        return geneId;
    }

    /**
     * @param geneId geneId
     */
    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    /**
     * @return geneIdDb
     */
    public String getGeneIdDb() {
        return geneIdDb;
    }

    /**
     * @param geneIdDb geneIdDb
     */
    public void setGeneIdDb(String geneIdDb) {
        this.geneIdDb = geneIdDb;
    }

    /**
     * @return geneIdDbVersion
     */
    public String getGeneIdDbVersion() {
        return geneIdDbVersion;
    }

    /**
     * @param geneIdDbVersion geneIdDbVersion
     */
    public void setGeneIdDbVersion(String geneIdDbVersion) {
        this.geneIdDbVersion = geneIdDbVersion;
    }

    /**
     * @return geneIdXrefId
     */
    public String getGeneIdXrefId() {
        return geneIdXrefId;
    }

    /**
     * @param geneIdXrefId geneIdXrefId
     */
    public void setGeneIdXrefId(String geneIdXrefId) {
        this.geneIdXrefId = geneIdXrefId;
    }

    /**
     * @return geneIdXrefDb
     */
    public String getGeneIdXrefDb() {
        return geneIdXrefDb;
    }

    /**
     * @param geneIdXrefDb geneIdXrefDb
     */
    public void setGeneIdXrefDb(String geneIdXrefDb) {
        this.geneIdXrefDb = geneIdXrefDb;
    }

    /**
     * @return tissueExpression
     */
    public TissueExpression getTissueExpression() {
        return tissueExpression;
    }

    /**
     * @param tissueExpression tissueExpression
     */
    public void setTissueExpression(TissueExpression tissueExpression) {
        this.tissueExpression = tissueExpression;
    }

    /**
     * @return subcellularLocation
     */
    public SubcellularLocation getSubcellularLocation() {
        return subcellularLocation;
    }

    /**
     * @param subcellularLocation subcellularLocation
     */
    public void setSubcellularLocation(SubcellularLocation subcellularLocation) {
        this.subcellularLocation = subcellularLocation;
    }

    /**
     * @return rnaExpression
     */
    public RnaExpression getRnaExpression() {
        return rnaExpression;
    }

    /**
     * @param rnaExpression rnaExpression
     */
    public void setRnaExpression(RnaExpression rnaExpression) {
        this.rnaExpression = rnaExpression;
    }

    /**
     * @return antibody
     */
    public Antibody getAntibody() {
        return antibody;
    }

    /**
     * @param antibody antibody
     */
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

        /**
         * @return type
         */
        public String getType() {
            return type;
        }
        /**
         * @param type type
         */
        public void setType(String type) {
            this.type = type;
        }
        /**
         * @return technology
         */
        public String getTechnology() {
            return technology;
        }
        /**
         * @param technology technology
         */
        public void setTechnology(String technology) {
            this.technology = technology;
        }
        /**
         * @return verification
         */
        public String getVerification() {
            return verification;
        }
        /**
         * @param verification verification
         */
        public void setVerification(String verification) {
            this.verification = verification;
        }
        /**
         * @return verificationType
         */
        public String getVerificationType() {
            return verificationType;
        }
        /**
         * @param verificationType verificationType
         */
        public void setVerificationType(String verificationType) {
            this.verificationType = verificationType;
        }
        /**
         * @return dataSet
         */
        public Set<TissueExpressionData> getDataSet() {
            return dataSet;
        }
        /**
         * @param dataSet dataSet
         */
        public void setDataSet(Set<TissueExpressionData> dataSet) {
            this.dataSet = dataSet;
        }
        /**
         * @return summarySet
         */
        public Set<TissueExpressionSummary> getSummarySet() {
            return summarySet;
        }
        /**
         * @param summarySet summarySet
         */
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
        /**
         * @return summary
         */
        public String getSummary() {
            return summary;
        }
        /**
         * @param summary summary
         */
        public void setSummary(String summary) {
            this.summary = summary;
        }
        /**
         * @return summaryType
         */
        public String getSummaryType() {
            return summaryType;
        }
        /**
         * @param summaryType summaryType
         */
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

        /**
         * @return tissue
         */
        public String getTissue() {
            return tissue;
        }
        /**
         * @param tissue tissue
         */
        public void setTissue(String tissue) {
            this.tissue = tissue;
        }
        /**
         * @return tissueStatus
         */
        public String getTissueStatus() {
            return tissueStatus;
        }
        /**
         * @param tissueStatus tissueStatus
         */
        public void setTissueStatus(String tissueStatus) {
            this.tissueStatus = tissueStatus;
        }
        /**
         * @return cellType
         */
        public String getCellType() {
            return cellType;
        }
        /**
         * @param cellType cellType
         */
        public void setCellType(String cellType) {
            this.cellType = cellType;
        }
        /**
         * @return levelSet
         */
        public Set<Level> getLevelSet() {
            return levelSet;
        }
        /**
         * @param levelSet levelSet
         */
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

        /**
         * @return level
         */
        public String getLevel() {
            return level;
        }
        /**
         * @param level level
         */
        public void setLevel(String level) {
            this.level = level;
        }
        /**
         * @return type
         */
        public String getType() {
            return type;
        }
        /**
         * @param type type
         */
        public void setType(String type) {
            this.type = type;
        }
        /**
         * @return count
         */
        public String getCount() {
            return count;
        }
        /**
         * @param count count
         */
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

        /**
         * @return type
         */
        public String getType() {
            return type;
        }
        /**
         * @param type type
         */
        public void setType(String type) {
            this.type = type;
        }
        /**
         * @return technology
         */
        public String getTechnology() {
            return technology;
        }
        /**
         * @param technology technology
         */
        public void setTechnology(String technology) {
            this.technology = technology;
        }
        /**
         * @return summary
         */
        public String getSummary() {
            return summary;
        }
        /**
         * @param summary summary
         */
        public void setSummary(String summary) {
            this.summary = summary;
        }
        /**
         * @return verification
         */
        public String getVerification() {
            return verification;
        }
        /**
         * @param verification verification
         */
        public void setVerification(String verification) {
            this.verification = verification;
        }
        /**
         * @return verificationType
         */
        public String getVerificationType() {
            return verificationType;
        }
        /**
         * @param verificationType verificationType
         */
        public void setVerificationType(String verificationType) {
            this.verificationType = verificationType;
        }
        /**
         * @return dataSet
         */
        public Set<SubcellularLocationData> getDataSet() {
            return dataSet;
        }
        /**
         * @param dataSet dataSet
         */
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

        /**
         * @return cellLine
         */
        public String getCellLine() {
            return cellLine;
        }
        /**
         * @param cellLine cellLine
         */
        public void setCellLine(String cellLine) {
            this.cellLine = cellLine;
        }
        /**
         * @return verification
         */
        public String getVerification() {
            return verification;
        }
        /**
         * @param verification verification
         */
        public void setVerification(String verification) {
            this.verification = verification;
        }
        /**
         * @return verificationType
         */
        public String getVerificationType() {
            return verificationType;
        }
        /**
         * @param verificationType verificationType
         */
        public void setVerificationType(String verificationType) {
            this.verificationType = verificationType;
        }
        /**
         * @return level
         */
        public String getLevel() {
            return level;
        }
        /**
         * @param level level
         */
        public void setLevel(String level) {
            this.level = level;
        }
        /**
         * @return levelType
         */
        public String getLevelType() {
            return levelType;
        }
        /**
         * @param levelType levelType
         */
        public void setLevelType(String levelType) {
            this.levelType = levelType;
        }
        /**
         * @return locSet
         */
        public Set<Location> getLocSet() {
            return locSet;
        }
        /**
         * @param locSet locSet
         */
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

        /**
         * @return location
         */
        public String getLocation() {
            return location;
        }
        /**
         * @param location location
         */
        public void setLocation(String location) {
            this.location = location;
        }
        /**
         * @return locationStatus
         */
        public String getLocationStatus() {
            return locationStatus;
        }
        /**
         * @param locationStatus locationStatus
         */
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

        /**
         * @return technology
         */
        public String getTechnology() {
            return technology;
        }
        /**
         * @param technology technology
         */
        public void setTechnology(String technology) {
            this.technology = technology;
        }
        /**
         * @return summary
         */
        public String getSummary() {
            return summary;
        }
        /**
         * @param summary summary
         */
        public void setSummary(String summary) {
            this.summary = summary;
        }
        /**
         * @return verification
         */
        public String getVerification() {
            return verification;
        }
        /**
         * @param verification verification
         */
        public void setVerification(String verification) {
            this.verification = verification;
        }
        /**
         * @return verificationType
         */
        public String getVerificationType() {
            return verificationType;
        }
        /**
         * @param verificationType verificationType
         */
        public void setVerificationType(String verificationType) {
            this.verificationType = verificationType;
        }
        /**
         * @return dataSet
         */
        public Set<RnaExpressionData> getDataSet() {
            return dataSet;
        }
        /**
         * @param dataSet dataSet
         */
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

        /**
         * @return cellLine
         */
        public String getCellLine() {
            return cellLine;
        }
        /**
         * @param cellLine cellLine
         */
        public void setCellLine(String cellLine) {
            this.cellLine = cellLine;
        }
        /**
         * @return level
         */
        public String getLevel() {
            return level;
        }
        /**
         * @param level level
         */
        public void setLevel(String level) {
            this.level = level;
        }
        /**
         * @return levelType
         */
        public String getLevelType() {
            return levelType;
        }
        /**
         * @param levelType levelType
         */
        public void setLevelType(String levelType) {
            this.levelType = levelType;
        }
        /**
         * @return levelfpkm
         */
        public String getLevelfpkm() {
            return levelfpkm;
        }
        /**
         * @param levelfpkm levelfpkm
         */
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
        private TissueExpression expression = new TissueExpression();
        private SubcellularLocation location = new SubcellularLocation();
        private String westernBlotTechnology;
        private String westernBlotVerificationType;
        private String westernBlotVerification;
        private String proteinArrayTechnology;
        private String proteinArrayVerificationType;
        private String proteinArrayVerification;

        /**
         * @return id
         */
        public String getId() {
            return id;
        }
        /**
         * @param id id
         */
        public void setId(String id) {
            this.id = id;
        }
        /**
         * @return releaseVersion
         */
        public String getReleaseVersion() {
            return releaseVersion;
        }
        /**
         * @param releaseVersion releaseVersion
         */
        public void setReleaseVersion(String releaseVersion) {
            this.releaseVersion = releaseVersion;
        }
        /**
         * @return releaseDate
         */
        public String getReleaseDate() {
            return releaseDate;
        }
        /**
         * @param releaseDate releaseDate
         */
        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }
        /**
         * @return antigenSequence
         */
        public String getAntigenSequence() {
            return antigenSequence;
        }
        /**
         * @param antigenSequence antigenSequence
         */
        public void setAntigenSequence(String antigenSequence) {
            this.antigenSequence = antigenSequence;
        }
        /**
         * @return tissueExpression
         */
        public TissueExpression getTissueExpression() {
            return expression;
        }
        /**
         * @param tissueExpression tissueExpression
         */
        public void setTissueExpression(TissueExpression tissueExpression) {
            this.expression = tissueExpression;
        }
        /**
         * @return subcellularLocation
         */
        public SubcellularLocation getSubcellularLocation() {
            return location;
        }
        /**
         * @param subcellularLocation subcellularLocation
         */
        public void setSubcellularLocation(SubcellularLocation subcellularLocation) {
            this.location = subcellularLocation;
        }
        /**
         * @return westernBlotTechnology
         */
        public String getWesternBlotTechnology() {
            return westernBlotTechnology;
        }
        /**
         * @param westernBlotTechnology westernBlotTechnology
         */
        public void setWesternBlotTechnology(String westernBlotTechnology) {
            this.westernBlotTechnology = westernBlotTechnology;
        }
        /**
         * @return westernBlotVerificationType
         */
        public String getWesternBlotVerificationType() {
            return westernBlotVerificationType;
        }
        /**
         * @param westernBlotVerificationType westernBlotVerificationType
         */
        public void setWesternBlotVerificationType(String westernBlotVerificationType) {
            this.westernBlotVerificationType = westernBlotVerificationType;
        }
        /**
         * @return westernBlotVerification
         */
        public String getWesternBlotVerification() {
            return westernBlotVerification;
        }
        /**
         * @param westernBlotVerification westernBlotVerification
         */
        public void setWesternBlotVerification(String westernBlotVerification) {
            this.westernBlotVerification = westernBlotVerification;
        }
        /**
         * @return proteinArrayTechnology
         */
        public String getProteinArrayTechnology() {
            return proteinArrayTechnology;
        }
        /**
         * @param proteinArrayTechnology proteinArrayTechnology
         */
        public void setProteinArrayTechnology(String proteinArrayTechnology) {
            this.proteinArrayTechnology = proteinArrayTechnology;
        }
        /**
         * @return proteinArrayVerificationType
         */
        public String getProteinArrayVerificationType() {
            return proteinArrayVerificationType;
        }
        /**
         * @param proteinArrayVerificationType proteinArrayVerificationType
         */
        public void setProteinArrayVerificationType(String proteinArrayVerificationType) {
            this.proteinArrayVerificationType = proteinArrayVerificationType;
        }
        /**
         * @return proteinArrayVerification
         */
        public String getProteinArrayVerification() {
            return proteinArrayVerification;
        }
        /**
         * @param proteinArrayVerification proteinArrayVerification
         */
        public void setProteinArrayVerification(String proteinArrayVerification) {
            this.proteinArrayVerification = proteinArrayVerification;
        }
    }
}
