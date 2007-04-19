<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
   version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns="http://www.w3.org/1999/xhtml">

  <!--
      <xsl:template match="section/title">
        <div class="heading2"><xsl:apply-templates/></div>
      </xsl:template>
      -->
  <xsl:template match="inlinemediaobject/imageobject/imagedata">
    <img alt="[{@fileref}]">
      <xsl:attribute name="class">
        <xsl:text>inlinegraphic</xsl:text>
        <xsl:if test="@align = 'center'">
          <xsl:text>-center</xsl:text>
        </xsl:if>
      </xsl:attribute>

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
      <xsl:apply-templates/>
    </img>
  </xsl:template>

  <xsl:template match="mediaobject/imageobject/imagedata">
    <img alt="[{@fileref}]">
      <xsl:attribute name="class">
        <xsl:text>inlinegraphic</xsl:text>
        <xsl:if test="@align = 'center'">
          <xsl:text>-center</xsl:text>
        </xsl:if>
      </xsl:attribute>

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
      <xsl:apply-templates/>
    </img>
  </xsl:template>

  <!-- ******* -->

  <xsl:template mode="copy-no-ns" match="*">
    <xsl:element name="{name(.)}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates mode="copy-no-ns"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="verbatim">
    <xsl:apply-templates mode="copy-no-ns"/>
  </xsl:template>

  <xsl:template match="article">
    <div class="docbook-page">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="book">
    <div class="docbook-page">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="articleinfo/title">
    <h2 class="title"><xsl:apply-templates/></h2>
  </xsl:template>

  <xsl:template match="author">
    <p class="author"><xsl:apply-templates/></p>
  </xsl:template>

  <xsl:template match="sect1/title">
    <h3><xsl:apply-templates/></h3>
  </xsl:template>

  <xsl:template match="sect2/title">
    <h4><xsl:apply-templates/></h4>
  </xsl:template>

  <xsl:template match="sect3/title">
    <h5><xsl:apply-templates/></h5>
  </xsl:template>

  <xsl:template match="firstterm">
    <i><xsl:apply-templates/></i>
  </xsl:template>

  <xsl:template match="itemizedlist">
    <ul><xsl:apply-templates/></ul>
  </xsl:template>

  <xsl:template match="orderedlist">
    <ol><xsl:apply-templates/></ol>
  </xsl:template>

  <xsl:template match="listitem">
    <li><xsl:apply-templates/></li>
  </xsl:template>

  <xsl:template match="table">
    <table class="{title/text()}"><xsl:apply-templates/></table>
  </xsl:template>

  <xsl:template match="table/title">
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
    <img alt="[{@fileref}]">
      <xsl:attribute name="class">
        <xsl:text>inlinegraphic</xsl:text>
        <xsl:if test="@align = 'center'">
          <xsl:text>-center</xsl:text>
        </xsl:if>
      </xsl:attribute>

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
      <xsl:if test="string-length(@align)!=0">
        <xsl:attribute name="align">
          <xsl:value-of select="@align"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="string-length(@width)!=0">
        <xsl:attribute name="width">
          <xsl:value-of select="@width"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:attribute name="border">0</xsl:attribute>
      <xsl:attribute name="hspace">7</xsl:attribute>
      <xsl:attribute name="vspace">5</xsl:attribute>
      <xsl:apply-templates/>
    </img>
  </xsl:template>

  <!--
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
          -->
  <!-- Icon, if present --> <!--
                                <xsl:choose>
                                  <xsl:when test="substring(@url, string-length(@url)-2, string-length(@url)) = 'sxi'">
                                    <img>
                                      <xsl:attribute name="border">0</xsl:attribute>
                                      <xsl:attribute name="hspace">5</xsl:attribute>
                                      <xsl:attribute name="src"><xsl:copy-of select="$basedir"/>/images/openoffice</xsl:attribute>
                                    </img>
                                  </xsl:when>
                                  <xsl:otherwise> -->
  <!-- no icon--> <!--
                      </xsl:otherwise>
                      </xsl:choose>

                      </a>
                      </xsl:template>
                    -->

  <xsl:template match="presentation">
    <xsl:apply-templates/>
    <ul class="fileformat">
      <li><a href="{@name}.sxi"><img src="{$basedir}/images/sxi.gif" alt="Format: Impress (OpenOffice.org)"/></a></li>
      <li><a href="{@name}.pdf"><img src="{$basedir}/images/pdf.gif" alt="Format: PDF (Acrobat)"/></a></li>
    </ul>
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

  <xsl:template match="listitem/para">
    <xsl:apply-templates/><br />
  </xsl:template>

  <xsl:template match="para">
    <p>
      <xsl:attribute name="style">
        <xsl:value-of select="@style"/>
      </xsl:attribute>
        
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="abstract | date | keywords | copyright"/>


  <xsl:template match="anchor">
    <a>
      <xsl:attribute name="name"><xsl:apply-templates/></xsl:attribute>
      <xsl:text> </xsl:text></a>
  </xsl:template>

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
      <xsl:choose>
          <xsl:when test="@role='bold'">
            <strong><xsl:apply-templates/></strong>
          </xsl:when>
          <xsl:otherwise>
            <i><xsl:apply-templates/></i>
          </xsl:otherwise>
        </xsl:choose>
  </xsl:template>

  <xsl:template match="subtitle">
    <h3><xsl:apply-templates/></h3>
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

  <xsl:template match="quote">
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

  <xsl:template match="sources[@version=$releaseversion]">
    <dl>
      <xsl:for-each select="source">
        <dt>
          <xsl:value-of select="name" disable-output-escaping="yes"/>
          <xsl:if test="(string-length(version))">
            <xsl:text> - </xsl:text><xsl:value-of select="version"/>
          </xsl:if>
        </dt>
        <dd>
          <xsl:value-of select="description" disable-output-escaping="yes"/>
        </dd>
      </xsl:for-each>
    </dl>
  </xsl:template>

  <xsl:template match="sources[@version!=$releaseversion]">
    <!-- show only the current version -->
  </xsl:template>

  <!--
      <xsl:template match="releaseinfo">
        <p align="right">
          <xsl:value-of select="substring('$',2)"/><text>-</text>
        </p>
      </xsl:template>
      -->

  <xsl:template match="nbsp">&#160;</xsl:template>

  <xsl:template match="span"><span class="{@class}"><xsl:apply-templates/></span></xsl:template>

</xsl:stylesheet>

