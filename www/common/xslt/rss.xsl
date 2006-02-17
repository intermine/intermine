<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">
  
  <xsl:template match="news">
    <xsl:variable name="id" select="@id"/>
    <xsl:variable name="limit" select="@limit"/>
    <xsl:variable name="items" select="document(concat('../../',$branding, '/', $brand/rss[@id=$id]/@file))/rss/channel"/>
    
    <xsl:for-each select="$items/item[position()&lt;$limit]">
      <div class="news-item">
        <div class="news-item-header">
          <xsl:if test="title">
            <div class="news-title"><xsl:value-of select="title"/></div>
          </xsl:if>
          <div class="news-date"><xsl:value-of select="pubDate"/></div>
        </div>
        <p><xsl:value-of select="description" disable-output-escaping="yes"/></p>
      </div>
    </xsl:for-each>
  </xsl:template>
  
</xsl:stylesheet>
