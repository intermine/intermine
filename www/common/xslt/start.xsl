<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">
  
  <xsl:template match="startbutton">
    <div align="center">
      <a class="startbutton" href="{xsl:concat($webappprefix,'/begin.do')}">
        <xsl:apply-templates/> <img src="images/right-arrow.gif" border="0" alt=""/>
      </a>
    </div>
  </xsl:template>
  
</xsl:stylesheet>
