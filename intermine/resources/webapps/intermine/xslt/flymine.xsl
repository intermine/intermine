<?xml version="1.0" encoding="iso-8859-1"?>

<xsl:stylesheet
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns="http://www.w3.org/1999/xhtml"
version="1.0">
 
<xsl:output method="xml" encoding="iso-8859-1" doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd"/>

<xsl:template match="artheader/title">
<h2 align="center"><xsl:apply-templates/></h2>
</xsl:template>

<xsl:template match="author">
<p align="center"><xsl:apply-templates/></p>
</xsl:template>

<xsl:template match="sect1/title">
<h3><xsl:apply-templates/></h3>
</xsl:template>

<xsl:template match="sect2/title">
<h4><xsl:apply-templates/></h4>
</xsl:template>

<xsl:template match="firstterm">
<i><xsl:apply-templates/></i>
</xsl:template>

<xsl:template match="itemizedlist">
<ul><xsl:apply-templates/></ul>
</xsl:template>

<xsl:template match="listitem">
<li><xsl:apply-templates/></li>
</xsl:template>

<xsl:template match="table">
<table><xsl:apply-templates/></table>
</xsl:template>

<xsl:template match="tbody">
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="row">
<tr><xsl:apply-templates/></tr>
</xsl:template>

<xsl:template match="entry">
<td><xsl:apply-templates/></td>
</xsl:template>

<xsl:template match="email">
<a>
<xsl:attribute name="href">mailto:<xsl:apply-templates/></xsl:attribute>
    <xsl:apply-templates/>
</a>
</xsl:template>

<xsl:template match="inlinegraphic">
<img>
<xsl:attribute name="src">
  <xsl:value-of select="@fileref"/>
</xsl:attribute>
<xsl:attribute name="border">0</xsl:attribute>
<xsl:apply-templates/>
</img>
</xsl:template>

<xsl:template match="graphic">
<img>
<xsl:attribute name="src">
  <xsl:value-of select="@fileref"/>
</xsl:attribute>
<xsl:if test="string-length(@align)!=0">
    <xsl:attribute name="align">
        <xsl:value-of select="@align"/>
    </xsl:attribute>
</xsl:if>
<xsl:attribute name="border">0</xsl:attribute>
<xsl:attribute name="hspace">7</xsl:attribute>
<xsl:attribute name="vspace">5</xsl:attribute>
<xsl:apply-templates/>
</img>
</xsl:template>

<xsl:template match="ulink">
  <a>
    <xsl:attribute name="href">
      <xsl:value-of select="@url"/>
    </xsl:attribute>

    <xsl:choose>
      <xsl:when test="count(child::node())=0">
        <xsl:value-of select="@url"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>

    <!-- Icon, if present -->
    <xsl:choose>
      <xsl:when test="substring(@url, string-length(@url)-2, string-length(@url)) = 'sxi'">
        <img>
          <xsl:attribute name="border">0</xsl:attribute>
          <xsl:attribute name="hspace">5</xsl:attribute>
          <xsl:attribute name="src">
            /images/openoffice
          </xsl:attribute>
        </img>
      </xsl:when>
      <xsl:otherwise>
      <!-- no icon-->
      </xsl:otherwise>
    </xsl:choose>

  </a>
</xsl:template>

<xsl:template match="url">
<a target="_blank">
<xsl:attribute name="href">
<xsl:apply-templates/>
</xsl:attribute>
<xsl:apply-templates/>
</a>
</xsl:template>

<xsl:template match="url[@protocol='mailto']">
<a>
<xsl:attribute name="href">mailto:<xsl:apply-templates/>
</xsl:attribute>
<xsl:apply-templates/>
</a>
</xsl:template>

<xsl:template match="para">
<p><xsl:apply-templates/></p>
</xsl:template>

<xsl:template match="abstract | date | keywords | copyright"/>

<xsl:template match="menu/section">
<p><xsl:apply-templates/></p>
</xsl:template>

<xsl:template match="menu/section/heading">
<font class="menu-heading"><xsl:apply-templates/></font><br />
</xsl:template>

<xsl:template match="menu/section/item">
<font class="menu-item"><xsl:apply-templates/></font><br/>
</xsl:template>

<xsl:template match="emphasis">
<i><xsl:apply-templates/></i>
</xsl:template>

<xsl:template match="highlights">
<b><xsl:apply-templates/></b>
</xsl:template>

<xsl:template match="filename">
<tt><xsl:apply-templates/></tt>
</xsl:template>

<xsl:template match="varname">
<code><xsl:apply-templates/></code>
</xsl:template>

<xsl:template match="command">
<code><xsl:apply-templates/></code>
</xsl:template>

<xsl:template match="programlisting">
<blockquote><pre><code><xsl:apply-templates/></code></pre></blockquote>
</xsl:template>

<xsl:template match="synopsis">
<code><xsl:apply-templates/></code>
</xsl:template>

<xsl:template match="function">
<code><xsl:apply-templates/></code>
</xsl:template>

<xsl:template match="classname">
<code><xsl:apply-templates/></code>
</xsl:template>

<xsl:template match="pubdate">
<xsl:variable name="releaseinfo"><xsl:value-of select="/article/artheader/releaseinfo"/></xsl:variable>
<p align="right">
<xsl:value-of select="substring($releaseinfo,2,string-length($releaseinfo)-2)"/><text> - </text>
<xsl:value-of select="substring(text(),16,2)"/><text>/</text>
<xsl:value-of select="substring(text(),13,2)"/><text>/</text>
<xsl:value-of select="substring(text(),8,4)"/>
</p>
</xsl:template>

<xsl:template match="releaseinfo"/>

<!--
<xsl:template match="releaseinfo">
<p align="right">
<xsl:value-of select="substring('$',2)"/><text>-</text>
</p>
</xsl:template>
-->

<xsl:template match="nbsp">&#160;</xsl:template>
</xsl:stylesheet>

