<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">
  
  <xsl:template match="sources">
    <xsl:variable name="imver" select="@version"/>
    <xsl:variable name="items" select="document(concat('../../',$branding, @sources))/intermine-sources/sources[@version=$imver]"/>
    <ul>
    <xsl:for-each select="$items/source">
      <li>
        <xsl:value-of select="description" disable-output-escaping="yes"/>
        <xsl:if test="(string-length(version))">
          <xsl:text> (Version: </xsl:text><xsl:value-of select="version"/><xsl:text>)</xsl:text>
        </xsl:if>
      </li>
    </xsl:for-each>
    </ul>
  </xsl:template>
  
</xsl:stylesheet>
