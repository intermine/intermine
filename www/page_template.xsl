<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0" 
  xmlns:ni="xalan://org.apache.xalan.lib.NodeInfo"
  exclude-result-prefixes="ni">
  
  <xsl:output
    method="xml"
    indent="yes"
    encoding="utf-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>

  <xsl:param name="basedir"/>
  <xsl:param name="branding"/>
  <xsl:variable name="brand" select="document(concat($branding,'/branding.xml'))/brand"/>
  <xsl:param name="webappprefix"/>
  <xsl:param name="outputext"/>
  <xsl:param name="sourceref"/>
<!--<xsl:variable name="source" select="substring-after(ni:systemId(),concat($sourceref,'/'))"/>-->

  <xsl:template match="/">
    <html>
      <head>
        <title><xsl:value-of select="$brand/title"/></title>
        <xsl:for-each select="$brand/stylesheet">
          <link rel="stylesheet" type="text/css" href="{concat($basedir, '/', @file)}"
                media="screen,printer"/>
        </xsl:for-each>
        <xsl:for-each select="$brand/meta">
          <meta>
            <xsl:copy-of select="@*"/>
          </meta>
        </xsl:for-each>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <!-- The ; below is for Netscape 4 (to avoid generating <script/>) -->
        <script type="text/javascript" src="{$basedir}/style/footer.js">;</script>
      </head>
      
      <body>
        <div id="header">
          <h1>
            <a href="{$basedir}/">
              <xsl:apply-templates mode="copy-no-ns" select="$brand/title/node()"/>
            </a>
          </h1>
          <p>
            <xsl:apply-templates mode="copy-no-ns" select="$brand/headline/node()"/>
          </p>
        </div>
        
        <!-- <p>NI: <xsl:value-of select="ni:systemId()"/></p>
             <p>Sourceref: <xsl:value-of select="$sourceref"/></p>
             <p>Source file: <xsl:value-of select="$source"/></p>
             <p>Menu path: <xsl:call-template name="menupath"/></p> -->
             

        <div id="pagecontent">
          <table id="static-table" width="100%">
            <tr>
              <td cellpadding="0" valign="top" id="sidebar" width="15%">
                <xsl:call-template name="sidebar"/>
              </td>
              <td cellpadding="0" valign="top" id="static-content" width="85%">
                <xsl:apply-templates/>
              </td>
            </tr>
          </table>
        </div>

        <div id="footer">
          <div id="address"><a href="mailto:info%5Bat%5Dflymine.org">info[at]flymine.org</a> - Tel: +44 (0)1223 333377 - University of Cambridge - UK</div>
          <div id="wellcome"><xsl:apply-templates mode="copy-no-ns" select="$brand/funding/node()"/></div>
        </div>
      </body>
    </html>
  </xsl:template>

  <xsl:template mode="copy-no-ns" match="*">
    <xsl:element name="{name(.)}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates mode="copy-no-ns"/>
    </xsl:element>
  </xsl:template>

</xsl:stylesheet>
