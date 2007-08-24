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

  <xsl:param name="projectcontact"/>

  <xsl:template match="/">

    <html>
      <head>
        <xsl:comment>#include virtual="/<xsl:value-of select="$webapppath"/>/htmlHead.do"</xsl:comment>
      </head>
   
      <body>
        <xsl:comment>#include virtual="/<xsl:value-of select="$webapppath"/>/headMenu.do"</xsl:comment>
        
        <div id="pagecontent">
          <table id="static-table" width="100%" cellspacing="0">
            <tr>
              <td valign="top" id="sidebar" width="5%">
                <xsl:call-template name="sidebar"/>
              </td>
              
              <td valign="top" width="95%">
                
                <div id="static-content">
                  <xsl:apply-templates mode="copy-no-ns"/>
                </div>
              </td>
            </tr>
          </table>
        </div>

<script src="http://www.google-analytics.com/urchin.js" type="text/javascript">
</script>
<script type="text/javascript">
_uacct = "UA-1566492-2";
urchinTracker();
</script>

      </body>
    </html>
  </xsl:template>



  <xsl:template mode="copy-no-ns" match="*">
    <xsl:element name="{name(.)}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates mode="copy-no-ns"/>
      <xsl:apply-templates select="news"/>
    </xsl:element>
  </xsl:template>

</xsl:stylesheet>
