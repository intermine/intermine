<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0" 
  xmlns="http://www.w3.org/1999/xhtml">
  
  <xsl:output
    method="xml"
    indent="yes"
    encoding="utf-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>

  <xsl:param name="basedir"/>
  <xsl:param name="branding"/>
  <xsl:variable name="brand" select="document(concat('../../',$branding,'/branding.xml'))/brand"/>
  <xsl:param name="webappprefix"/>
  <xsl:param name="outputext"/>

  <xsl:template match="/">
    <html>
      <head>
        <title><xsl:value-of select="$brand/title"/></title>
        <xsl:for-each select="$brand/stylesheet">
          <link rel="stylesheet" type="text/css" href="{concat($basedir, '/', @file)}" media="screen,print"/>
        </xsl:for-each>
        <xsl:for-each select="$brand/meta">
          <meta>
            <xsl:copy-of select="@*"/>
          </meta>
        </xsl:for-each>
        <xsl:for-each select="$brand/rss">
            <link rel="alternate" type="application/rss+xml" href="{concat($basedir, '/', @file)}" title="News"/>
        </xsl:for-each>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <script type="text/javascript" src="{$basedir}/style/footer.js">;</script>
      </head>
      
      <body>
        
        <div id="header">
          <table width="100%" cellspacing="0" cellpadding="0">
            <tr>
              <td width="1%" align="center" valign="center">
                <a href="{$basedir}/">
                  <img src="{$basedir}/images/logo.png" border="0" id="logo">
                    <xsl:attribute name="alt">
                      <xsl:value-of select="$brand/title"/>
                    </xsl:attribute>
                  </img>
                </a>
              </td>
              <td width="99%" valign="center">
                <img src="{$basedir}/images/title.png" border="0" id="title">
                  <xsl:attribute name="alt">
                    <xsl:value-of select="$brand/title"/>
                  </xsl:attribute>
                </img>
                <p>
                  <xsl:apply-templates mode="copy-no-ns" select="$brand/headline/node()"/>
                </p>
              </td>
            </tr>
          </table>
        </div>
        
        <div id="pagecontent">
          <table id="static-table" width="100%" cellspacing="0">
            <tr>
              <td valign="top" id="sidebar" width="5%">
                <xsl:call-template name="sidebar"/>
              </td>
              
              <td valign="top" width="95%">
                
                <div id="static-content">
                  <xsl:apply-templates/>
                </div>
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
