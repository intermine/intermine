package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.DecimalFormat;

/**
 * A Java class to store cell line and developmental stage score info for gene or exon.
 *
 * @author Sergio
 *
 */
public class ExpressionScore
{
    // the experiment condition name
    private String condition;
    // the expression score
    private Double score;
    // the log score
    private Double logScore;
    // the feature's primaryId
    private String primaryId;
    // the feature's symbol
    private String symbol;

    DecimalFormat df = new DecimalFormat("#.##");

    /**
     * Constructor.
     * @param condition the experiment condition name
     * @param score the expression score
     * @param primaryId the feature's primaryId
     * @param symbol the feature's symbol
     */
    public ExpressionScore(String condition, Double score, String primaryId, String symbol) {
        this.condition = condition;
        this.score = score;
        this.primaryId = primaryId;

        if (symbol == null) {
            this.symbol = primaryId;
        } else {
            this.symbol = symbol;
        }

        this.logScore = log2Score(score);
    }

    /**
     * Default Constructor.
     */
    public ExpressionScore() {
        super();
    }

    /**
     * Calculate logarithm 2 of expression score.
     *
     * @param score expression score
     * @return log2(score)
     */
    private Double log2Score(Double score) {
        return Double.valueOf(df.format(Math.log(score + 1) / Math.log(2)));
    }

    /**
     * @return the experimentType
     */
    public Double getScore() {
        return score;
    }

    /**
     * @return the experimentType
     */
    public String getCondition() {
        return condition;
    }

    /**
     * @return the primaryId
     */
    public String getPrimaryId() {
        return primaryId;
    }

    /**
     * @param primaryId the primaryId to set
     */
    public void setPrimaryId(String primaryId) {
        this.primaryId = primaryId;
    }

    /**
     * @return the symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @param symbol the symbol to set
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
     * @return the logScore
     */
    public Double getLogScore() {
        return logScore;
    }
}
