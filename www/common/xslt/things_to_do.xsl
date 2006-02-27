<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">
  
  <xsl:template match="things-to-do">
    <ul id="todo">
      <li>
        <a href="{xsl:concat($webappprefix,'/classChooser.do')}">
          List all classes...
        </a>
      </li>
      <li>
        <a href="{xsl:concat($webappprefix,'/tree.do')}">
          Browse model...
        </a>
      </li>
      <li>
        <a href="{xsl:concat($webappprefix,'/importQuery.do')}">
          Import a query from XML...
        </a>
      </li>
    </ul>
  </xsl:template>
  
</xsl:stylesheet>
