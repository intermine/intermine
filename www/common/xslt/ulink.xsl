<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml"
  version="1.0">

  <xsl:output
    method="xml"
    indent="yes"
    encoding="utf-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>

  <xsl:template match="ulink">
    <a href="{@url}">
      <xsl:choose>
        <xsl:when test="substring(@url, string-length(@url)-3) = '.sxi'">
          <xsl:attribute name="class">openoffice</xsl:attribute>
        </xsl:when>
        <xsl:when test="substring(@url, string-length(@url)-2) = '.do'">
          <xsl:attribute name="href">
            <xsl:value-of select="$webappprefix"/><xsl:value-of select="@url"/>
          </xsl:attribute>
        </xsl:when>
        <xsl:when test="substring(@url, string-length(@url)-3) = '.xml'">
          <xsl:attribute name="href">
            <xsl:value-of select="substring(@url,1,string-length(@url)-3)"/>
            <xsl:value-of select="$outputext"/>
          </xsl:attribute>
        </xsl:when>
      </xsl:choose>

      <xsl:if test="string-length(@target) > 0">
        <xsl:attribute name="target">
          <xsl:value-of select="@target"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="string-length(@onclick) > 0">
        <xsl:attribute name="onclick">
          <xsl:value-of select="@onclick"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="count(child::node())=0">
          <xsl:value-of select="@url"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>

    </a>
  </xsl:template>
</xsl:stylesheet>
