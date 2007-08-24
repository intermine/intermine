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

  <xsl:param name="basedir"/>
  <xsl:param name="branding"/>
  <xsl:param name="webappprefix"/>
  <xsl:param name="webapppath"/>
  <xsl:param name="outputext"/>
  <xsl:param name="releaseversion"/>

  <xsl:variable name="brand" select="document(concat('../../',$branding,'/branding.xml'))/brand"/>

  <xsl:include href="menu.xsl"/>
  <xsl:include href="rss.xsl"/>
  <xsl:include href="new_page_template.xsl"/>
  
</xsl:stylesheet>
