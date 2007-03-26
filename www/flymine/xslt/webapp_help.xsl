<?xml version='1.0' encoding="utf-8"?>
<xsl:stylesheet
   xmlns="http://www.w3.org/1999/xhtml"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   version='1.0'>

  <xsl:import href="/software/noarch/docbook-xsl/html/chunk.xsl"/>

  <xsl:include href="../../common/xslt/ulink.xsl"/>
  <xsl:include href="../../common/xslt/menu.xsl"/>
  <xsl:include href="../../common/xslt/help_page_template.xsl"/>

  <!-- copied so that we can add header, sidebar and pagecontent DIVs -->
  <xsl:template name="chunk-element-content">
    <xsl:param name="prev"/>
    <xsl:param name="next"/>
    <xsl:param name="nav.context"/>
    <xsl:param name="content">
      <xsl:apply-imports/>
    </xsl:param>

    <xsl:call-template name="user.preroot"/>

    <html>
      <xsl:call-template name="html.head">
        <xsl:with-param name="prev" select="$prev"/>
        <xsl:with-param name="next" select="$next"/>
      </xsl:call-template>

      <body>
        <script type="text/javascript" src="{$basedir}/style/site.js">;</script>
        <div id="pagecontent">
          <table id="static-table" width="100%" cellpadding="0" >
            <tr>
              <td id="static-content" valign="top" width="85%">
                <xsl:call-template name="body.attributes"/>
                <xsl:call-template name="user.header.navigation"/>

                <xsl:call-template name="header.navigation">
	          <xsl:with-param name="prev" select="$prev"/>
	          <xsl:with-param name="next" select="$next"/>
	          <xsl:with-param name="nav.context" select="$nav.context"/>
                </xsl:call-template>

                <xsl:call-template name="user.header.content"/>

                <xsl:copy-of select="$content"/>

                <xsl:call-template name="user.footer.content"/>

                <xsl:call-template name="footer.navigation">
	          <xsl:with-param name="prev" select="$prev"/>
	          <xsl:with-param name="next" select="$next"/>
	          <xsl:with-param name="nav.context" select="$nav.context"/>
                </xsl:call-template>

                <xsl:call-template name="user.footer.navigation"/>
              </td>
            </tr>
          </table>
        </div>

<script src="http://www.google-analytics.com/urchin.js"
type="text/javascript">
</script>
<script type="text/javascript">
_uacct = "UA-602790-3";
urchinTracker();
</script>

      </body>
    </html>
  </xsl:template>

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
