<?xml version='1.0' encoding="utf-8"?>
<xsl:stylesheet
   xmlns="http://www.w3.org/1999/xhtml"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   version='1.0'>

  <xsl:import href="/software/noarch/docbook-xsl/html/docbook.xsl"/>

  <xsl:include href="../../common/xslt/ulink.xsl"/>
  <xsl:include href="../../common/xslt/menu.xsl"/>
  <xsl:include href="../../common/xslt/page_template.xsl"/>


  <!-- inserted in the HEAD element -->
  <xsl:template name="user.head.content">
    <xsl:for-each select="$brand/stylesheet">
      <link rel="stylesheet" type="text/css" href="{concat($basedir, '/', @file)}"
            media="screen,printer"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="gentext.nav.home">
    Table of contents
  </xsl:template>
</xsl:stylesheet>
