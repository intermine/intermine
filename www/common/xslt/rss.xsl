<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">

  <!-- Found on http://www.xml.com/pub/a/2002/06/05/transforming.html
      (disabled-escaping added for FlyMine -->
  <xsl:template name="globalReplace">
    <xsl:param name="outputString"/>
    <xsl:param name="target"/>
    <xsl:param name="replacement"/>
    <xsl:choose>
      <xsl:when test="contains($outputString,$target)">
   
        <xsl:value-of select=
          "concat(substring-before($outputString,$target),
                 $replacement)" disable-output-escaping="yes"/>
        <xsl:call-template name="globalReplace">
          <xsl:with-param name="outputString" 
               select="substring-after($outputString,$target)"/>
          <xsl:with-param name="target" select="$target"/>
          <xsl:with-param name="replacement" 
               select="$replacement"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$outputString" disable-output-escaping="yes"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="news">
    <xsl:variable name="id" select="@id"/>
    <xsl:variable name="limit" select="@limit"/>
    <xsl:variable name="items" select="document(concat('../../',$branding, '/', $brand/rss[@id=$id]/@file))/rss/channel"/>
    
    <xsl:for-each select="$items/item[position()&lt;=$limit]">
      <div class="news-item">
        <div class="news-item-header">
          <xsl:if test="title">
            <h3><xsl:value-of select="title"/></h3>
          </xsl:if>
          <i><xsl:value-of select="pubDate"/></i>
        </div>
        <p><xsl:call-template name="globalReplace">
             <xsl:with-param name="outputString" select="description"/>
             <xsl:with-param name="target" select="'http://www.flymine.org'"/>
             <xsl:with-param name="replacement" select="$basedir"/>
           </xsl:call-template></p>
      </div>
    </xsl:for-each>
  </xsl:template>
  
</xsl:stylesheet>
