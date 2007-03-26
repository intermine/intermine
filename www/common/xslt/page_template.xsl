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
  <xsl:param name="webappprefix"/>
  <xsl:param name="projectcontact"/>
  <xsl:param name="releaseversion"/>
  <xsl:param name="outputext"/>

  <xsl:variable name="brand" select="document(concat('../../',$branding,'/branding.xml'))/brand"/>

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
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        </meta>
        <script type="text/javascript" src="{$basedir}/style/site.js">;</script>
      </head>
      
      <body>
        <div id="header">
          <div id="topright">
            <span class="version">
              Version <xsl:value-of select="$releaseversion"/>
            </span>
            <div class="contact">
              <xsl:value-of select="$projectcontact" disable-output-escaping="yes"/>
            </div>
            <div class="wellcome">
              <xsl:apply-templates mode="copy-no-ns" select="$brand/funding/node()"/>
            </div>
          </div>
        
          <a href="{$basedir}/">
            <img src="{$basedir}/images/logo.png" border="0" id="logo">
              <xsl:attribute name="alt">
                <xsl:value-of select="$brand/title"/>
              </xsl:attribute>
            </img>
          </a>
          <div id="title">
            <h1>
              <a href="{$basedir}/">
                <xsl:apply-templates mode="copy-no-ns" select="$brand/title/node()"/>
              </a>
            </h1>
            <p>
              <xsl:apply-templates mode="copy-no-ns" select="$brand/headline/node()"/>
            </p>
          </div>
          <div class="clear-both">.</div>
        </div>
        
        <div class="links">
          <span class="menu-item">
            <a href="/">
              Home
            </a>
          </span>
          <span class="menu-item">
            <script type="text/javascript">
              if (readCookie('have-query-<xsl:value-of select="$releaseversion"/>') == 'true') {
                document.write(linkTo('<xsl:value-of select="concat($webappprefix,'/query.do')"/>', 'Current query'));
              } else {
                document.write('Current query');
              }
            </script>
          </span>
          <span class="menu-item">
            <a href="{xsl:concat($webappprefix,'/mymine.do')}">
              MyMine
            </a>
          </span>
          <span class="menu-item">
            <a href="{xsl:concat($webappprefix,'/mymine.do?page=bags')}">
              Bags
            </a>
          </span>
          <span class="menu-item">
            <a href="{xsl:concat($webappprefix,'/templateSearch.do')}">
              Search templates
            </a>
            <img src="{xsl:concat($webappprefix,'/images/inspect.gif')}" width="12" height="11" alt="-&gt;"/>
          </span>
          <span class="menu-item">
            <a href="{xsl:concat($webappprefix,'/history.do')}">
              Query History
            </a>
          </span>
          <span class="menu-item">
            <a href="{xsl:concat($webappprefix,'/feedback.do')}">
              Feedback form
            </a>
          </span>
          <span class="menu-item">
            <script type="text/javascript">
              if (readCookie('logged-in-<xsl:value-of select="$releaseversion"/>') == 'true') {
                document.write(linkTo('<xsl:value-of select="concat($webappprefix,'/logout.do')"/>', 'Log out'));
              } else {
                document.write(linkTo('<xsl:value-of select="concat($webappprefix,'/login.do')"/>', 'Log in'));
              }
            </script>
          </span>
          <span class="menu-item">
            <a href="/doc/manual/">
              Help
            </a>
          </span>
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

  <xsl:template mode="copy-no-ns" match="*">
    <xsl:element name="{name(.)}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates mode="copy-no-ns"/>
    </xsl:element>
  </xsl:template>

</xsl:stylesheet>
