<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">

<xsl:variable name="menu" select="document(concat('../../',$branding,'/menu.xml'))/menu"/>

<xsl:template name="sidebar">
  <xsl:for-each select="$menu/item">

    <xsl:if test="@class">
      <xsl:attribute name="class"><xsl:value-of select="@class"/></xsl:attribute>
    </xsl:if>

    <xsl:if test="@title='Internal'">
      <xsl:comment>#if expr="$REMOTE_ADDR=/^192.168.128./" </xsl:comment>
    </xsl:if>
    <div class="heading">
      <xsl:value-of select="@title"/>
    </div>

    <div class="body">
      <!-- list menu sub-elements -->
      <xsl:if test="item">
        <ul>
          <!-- process news items -->
          <xsl:for-each select="news">
            <xsl:variable name="id" select="@id"/>
            <xsl:variable name="limit" select="@limit"/>
            <xsl:variable name="items" select="document(concat('../../',$branding, '/', $brand/rss[@id=$id]/@file))/rss/channel"/>
            
            <xsl:for-each select="$items/item[position()&lt;=$limit]">
              <li>
                <xsl:if test="title">
                  <div>
                    <a>
                      <xsl:attribute name="href">
                        <xsl:value-of select="$basedir"/>/news.shtml
                      </xsl:attribute>
                      <xsl:value-of select="title"/>
                    </a>
                  </div>
                  <div class="news-date">(<xsl:value-of select="pubDate"/>)</div>
                </xsl:if>
              </li>
            </xsl:for-each>
          </xsl:for-each>
          <xsl:for-each select="item">
            <li>
                <!-- Display the link after alteration -->
                <xsl:call-template name="menulink">
                  <xsl:with-param name="url" select="@url"/>
                  <xsl:with-param name="title" select="@title"/>
                </xsl:call-template>
            </li>
          </xsl:for-each>
        </ul>
      </xsl:if>
    </div>

    <xsl:if test="@title='Internal'">
      <xsl:comment>#endif </xsl:comment>
    </xsl:if>
  </xsl:for-each>
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

    <xsl:attribute name="class">
      <xsl:choose>
        <xsl:when test="substring(@url, string-length(@url)-2) = '.do'">
          <xsl:text>webapplink</xsl:text>
        </xsl:when>
      </xsl:choose>
    </xsl:attribute>
    
    <xsl:value-of select="$title"/>
  </a>
</xsl:template>

</xsl:stylesheet>
