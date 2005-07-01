<?xml version='1.0' encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

<!-- This sets the extension for HTML files to ".html".     -->
<xsl:param name="html.ext" select="'.@OUTPUT_EXTENSION@'"/>

<!-- This sets the filename based on the ID.                -->
<xsl:param name="use.id.as.filename" select="'1'"/>

<xsl:param name="css.decoration" select="'0'"/>

<xsl:param name="basedir" select="'@BASEDIR@'" />
<xsl:param name="webappprefix" select="'@WEBAPP_PREFIX@'"/>
<xsl:param name="outputext" select="'@OUTPUT_EXTENSION@'"/>
<xsl:param name="branding" select="'@BRANDING@'"/>
</xsl:stylesheet>
