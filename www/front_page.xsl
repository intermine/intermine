<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns="http://www.w3.org/1999/xhtml"
   version="1.0"
   xmlns:ni="xalan://org.apache.xalan.lib.NodeInfo"
   exclude-result-prefixes="ni">

  <xsl:import href="menu.xsl"/>
  <xsl:import href="ulink.xsl"/>
  <xsl:import href="page_template.xsl"/>

  <xsl:output
    method="xml"
    indent="yes"
    encoding="utf-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>

  <xsl:param name="basedir"/>
  <xsl:param name="branding"/>
  <xsl:param name="webappprefix"/>
  <xsl:param name="outputext"/>
  <xsl:param name="sourceref"/>

  <xsl:template match="article">
    <div class="docbook-page">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="sect1">
    <div class="box">
      <div class="heading2"><xsl:apply-templates select="title"/></div>
      <div class="body">
        <xsl:apply-templates select="para"/>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="para">
    <p><xsl:apply-templates/></p>
  </xsl:template>

  <xsl:template match="itemizedlist">
    <ul><xsl:apply-templates/></ul>
  </xsl:template>

  <xsl:template match="listitem">
    <li><xsl:apply-templates/></li>
  </xsl:template>

</xsl:stylesheet>
