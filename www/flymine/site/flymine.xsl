<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns="http://www.w3.org/TR/REC-html40"
version="1.0">

<xsl:output method="html"/>
<xsl:param name="basedir" />

<xsl:template match="/">
<html>
<head>
<title>FlyMine</title>
<link rel="stylesheet" type="text/css">
<xsl:attribute name="href">
<xsl:value-of select="concat($basedir, '/flymine.css')"/>
</xsl:attribute>
</link>
<meta name="keywords" content="microarray, bioinformatics, drosophila, genomics" />
<meta name="description" content="Integrated queryable database for Drosophila and Anopheles genomics" />
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
</head>
<body>

<table align="center" width="100%" border="0" cellpadding="10">
    <tr>
        <td colspan="2" align="left" valign="bottom">
            <font class="title">FlyMine</font>
            <br /><font class="subtitle">An integrated database for <i>Drosophila</i> and <i>Anopheles</i> genomics</font>
            <br /><hr width="100%" align="center" />
        </td>
    </tr>

    <tr>
        <td class="sidebar" height="10%" width="15%" align="left" valign="top" vspace="0" hspace="0">
            <xsl:apply-templates select="document('sidebar.xml')/*" />
        </td>
        <td class="main" align="left" valign="top" rowspan="2">
            <xsl:apply-templates/>
        </td>
    </tr>
    <tr>
        <td>
        </td>
    </tr>

    <tr vspace="0" hspace="0">
        <td class="footer" align="center" colspan="2" border="0" hspace="0" vspace="0">
	    <table class="footer">
                <tr>
                    <td align="left" width="10%">
                        <table class="footer">
                            <tr>
                                <td>
                                    <a href="http://www.wellcome.ac.uk">
                                        <img>
                                            <xsl:attribute name="src">
                                                <xsl:value-of select="concat($basedir, '/wellcome.gif')"/>
                                            </xsl:attribute>
                                            <xsl:attribute name="align">
                                                <xsl:value-of select="center"/>
                                            </xsl:attribute>
                                            <xsl:attribute name="border">
                                                <xsl:value-of select="0"/>
                                            </xsl:attribute>
                                            <xsl:attribute name="hspace">
                                                <xsl:value-of select="10"/>
                                            </xsl:attribute>
                                        </img>
                                    </a>
                                </td>
                                <td>
                                    FlyMine is funded by<br/>The Wellcome Trust<br/>(Grant no. 067205)
                                </td>
                            </tr>
                        </table>
                    </td>
                    <td align="right" width="10%">
                        <table class="footer" cellpadding="10">
                            <tr>
                                <td>
                                    FlyMine<br/>Department of Genetics<br/>Downing Street<br/>Cambridge, CB2 3EH, UK
                                </td>
                                <td>
                                    Tel: +44 (0)1223 333965<br/>Fax: +44 (0)1223 333992<br/><a href="mailto:info[at]flymine.org">info[at]flymine.org</a>
                                </td>
                            </tr>
                        </table>
                    </td>
                 </tr>
            </table>
        </td>
    </tr>

</table>

</body>
</html>
</xsl:template>

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
<xsl:choose>
    <xsl:when test="substring(@fileref, 1, 1) = '/'">
            <xsl:copy-of select="$basedir"/>
            <xsl:value-of select="@fileref"/>
    </xsl:when>
    <xsl:otherwise>
        <xsl:value-of select="@fileref"/>
    </xsl:otherwise>
</xsl:choose>
</xsl:attribute>
<xsl:attribute name="border">0</xsl:attribute>
<xsl:apply-templates/>
</img>
</xsl:template>

<xsl:template match="graphic">
<img>
<xsl:attribute name="src">
<xsl:choose>
    <xsl:when test="substring(@fileref, 1, 1) = '/'">
        <xsl:copy-of select="$basedir"/>
        <xsl:value-of select="@fileref"/>
    </xsl:when>
    <xsl:otherwise>
        <xsl:value-of select="@fileref"/>
    </xsl:otherwise>
</xsl:choose>
</xsl:attribute>
<xsl:attribute name="align">
    <xsl:value-of select="@align"/>
</xsl:attribute>
<xsl:attribute name="border">0</xsl:attribute>
<xsl:apply-templates/>
</img>
</xsl:template>

<xsl:template match="ulink">
<a>
<xsl:attribute name="href">
<xsl:choose>
    <xsl:when test="substring(@url, 1, 1) = '/'">
            <xsl:copy-of select="$basedir"/>
            <xsl:value-of select="@url"/>
    </xsl:when>
    <xsl:otherwise>
        <xsl:value-of select="@url"/>
    </xsl:otherwise>
</xsl:choose>
</xsl:attribute>



    <xsl:choose>
      <xsl:when test="count(child::node())=0">
        <xsl:value-of select="@url"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
</a>
</xsl:template>

<xsl:template match="url">
<A TARGET="_blank">
<xsl:attribute name="HREF">
<xsl:apply-templates/>
</xsl:attribute>
<xsl:apply-templates/>
</A>
</xsl:template>

<xsl:template match="url[@protocol='mailto']">
<A>
<xsl:attribute name="HREF">mailto:<xsl:apply-templates/>
</xsl:attribute>
<xsl:apply-templates/>
</A>
</xsl:template>

<xsl:template match="para">
<p><xsl:apply-templates/></p>
</xsl:template>

<xsl:template match="abstract | date | keywords | copyright"/>

<xsl:template match="menu/section">
<p><xsl:apply-templates/></p>
</xsl:template>

<xsl:template match="menu/section/heading">
<div><font class="menu-heading"><xsl:apply-templates/></font></div>
</xsl:template>

<xsl:template match="menu/section/item">
<font class="menu-item"><xsl:apply-templates/></font><br/>
</xsl:template>

<xsl:template match="emphasis">
<i><xsl:apply-templates/></i>
</xsl:template>

<xsl:template match="synopsis">
<code><xsl:apply-templates/></code>
</xsl:template>

</xsl:stylesheet>

