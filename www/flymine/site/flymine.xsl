<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns="http://www.w3.org/TR/REC-html40"
version="1.0">

<xsl:output method="html"/>

<xsl:template match="/">
<html>
<head>
<title>FlyMine</title>
<link rel="stylesheet" href="flymine.css" type="text/css" />
<meta name="keywords" content="microarray, bioinformatics, drosophila, genomics" />
<meta name="description" content="Integrated queryable database for Drosophila and Anopheles genomics" />
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
</head>
<body>

<table border="0" cellspacing="5" cellpadding="5" align="center" width="90%">
<tr>
<!-- <td align="center" valign="top">
<a href="index.html"><img src="logo.jpg" alt="FlyMine" border="0"></a>
</td> -->
<td align="left" valign="bottom" width="90%">
<font size="+2" color="green"><b>FlyMine</b></font>

<br /><font size="+1">An integrated database for <i>Drosophila</i> and <i>Anopheles</i> genomics</font>
<br /><hr width="100%" align="center" />
</td>
</tr>
</table>

<table border="0" cellspacing="5" cellpadding="5" align="center" width="90%">
<tr>
<td valign="top" colspan="2">

<table align="center" width="100%" border="0" cellpadding="10">
<tr>
<td align="left" valign="top">

<xsl:apply-templates/>

</td>
</tr>
</table>
</td>
</tr>

<tr><td align="center"><a href="http://www.wellcome.ac.uk"><img src="wellcome.gif" align="center" border="0" hspace="10" /></a>FlyMine is funded by The Wellcome Trust (Grant no. 067205)</td></tr>

</table>

<table border="1" cellspacing="0" cellpadding="5" align="center" width="90%">
  <tr>
    <td colspan="2" bgcolor="#ccffcc" align="center">
      FlyMine, Department of Genetics, Downing Street, Cambridge, CB2 3EH, UK
      <br />Tel: +44 (0)1223 333965.  Fax: +44 (0)1223 333992.  <a href="mailto:info[at]flymine.org">info[at]flymine.org</a>
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

<xsl:template match="itemizedlist">
<ul><xsl:apply-templates/></ul>
</xsl:template>

<xsl:template match="listitem">
<li><xsl:apply-templates/></li>
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

</xsl:stylesheet>

