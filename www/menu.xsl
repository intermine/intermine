<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml"> 

<xsl:variable name="menu" select="document(concat($branding,'/menu.xml'))/menu"/>
<xsl:variable name="menupath"><xsl:call-template name="menupath"/></xsl:variable>
<xsl:variable name="current" select="$menu//item[@url=$menupath]"/>

<xsl:template name="sidebar">
  <div id="sidebar">
    <ul>
      <xsl:for-each select="$menu/item">
        <li>
          <xsl:if test="@class">
            <xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute>
          </xsl:if>
          <!-- id="current" is used for by the css -->
          <xsl:if test="$menupath = @url or substring($menupath,1,string-length(@match)) = @match">
            <xsl:attribute name="id">current</xsl:attribute>
          </xsl:if>

          <!-- Display the link after alteration -->
          <xsl:call-template name="menulink">
            <xsl:with-param name="url" select="@url"/>
            <xsl:with-param name="title" select="@title"/>
          </xsl:call-template>

          <!-- "open" the menu is current page or contains a current page -->
          <xsl:if test="($menupath = @url and count(child::node()) != 0) or $current/../@url = @url">
            <ul>
              <xsl:for-each select="item">
                <li>
                  <!-- id="current" is used for by the css -->
                  <xsl:if test="$menupath = @url">
                    <xsl:attribute name="id">current</xsl:attribute>
                  </xsl:if>

                  <!-- Display the link after alteration -->
                  <xsl:call-template name="menulink">
                    <xsl:with-param name="url" select="@url"/>
                    <xsl:with-param name="title" select="@title"/>
                  </xsl:call-template>

                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>
          
        </li>
      </xsl:for-each>
    </ul>
  </div>
</xsl:template>

<xsl:template name="menupath">
  <xsl:choose>
    <xsl:when test="string-length(substring-after(substring-after($source,'/'),'/')) = 0">
      <xsl:value-of select="concat('/',$source)"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="concat('/',substring-before($source,substring-after(substring-after($source,'/'),'/')),'index.xml')"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="menulink">
  <xsl:param name="url"/>
  <xsl:param name="title"/>
  <a>
    <xsl:attribute name="href">
      <xsl:choose>
        <xsl:when test="starts-with($url,'http')">
          <xsl:value-of select="$url"/>
        </xsl:when>
        <xsl:when test="substring(@url, string-length(@url)-3) = '.xml'">
          <xsl:value-of select="$basedir"/>
          <xsl:value-of select="substring(@url,1,string-length(@url)-3)"/>
          <xsl:value-of select="$outputext"/>
        </xsl:when>
        <xsl:when test="substring(@url, string-length(@url)-2) = '.do'">
          <xsl:value-of select="$webappprefix"/><xsl:value-of select="@url"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$url"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    
    <xsl:value-of select="$title"/>
  </a>
</xsl:template>

</xsl:stylesheet>
