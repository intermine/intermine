<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml />

<link rel="stylesheet" href="model/css/frontpage_sections.css" type="text/css" media="screen" title="no title" charset="utf-8">

<div class="body">

<!-- Entry point section -->
<div align="center">

<table cellpadding="0" cellspacing="0" border="0" class="topBar hints">
<tr><th align="center"><h3>Try out the new <a href="/${WEB_PROPERTIES['webapp.path']}/spanUploadOptions.do?">Genomic Regions Search</a><h3></th></tr>
</table>


<!--
<h4>Try out the new <a href="/${WEB_PROPERTIES['webapp.path']}/spanUploadOptions.do?">Genomic Regions Search</a>
</h4>

<table cellpadding="0" cellspacing="0" border="0" class="projects" id="projects">
<tr><th align="center">modMine 18 contains the 15 February 2010 data freeze</th></tr>
</table>
-->
<div class="frontpage_sections">
    <ol>
        <li>
        <div>
        <h3>Search modMine</h3><br/>
          Search for modENCODE submissions by metadata
          <p>
            All fields will be searched, for example: antibody names <i>(e.g. 'PolII', 'H3K4me1', 'CP190')</i>,
            lab names <i>(e.g. 'Reinke', 'Snyder')</i>, data types <i>(e.g. 'UTR', 'bindingsite')</i>
          </p>
          <!--
            <ul>
                <li>For example antibody names (CP190, H3K4me1) or data types (bindingsite, UTR)</li>
                <li>Use AND to combine: <a href="/${WEB_PROPERTIES['webapp.path']}/modMineSearchResults.do?searchTerm=fly+AND+embryo">fly AND embryo</a></li>
                <li>Use AND NOT to exclude: <a href="/${WEB_PROPERTIES['webapp.path']}/modMineSearchResults.do?searchTerm=fly+AND+NOT+embryo">fly AND NOT embryo</a></li>
            </ul>
            -->
         <html:form action="/modMineSearchAction" focus="searchTerm">
            <input name="searchTerm" type="text" class="qs_input">
            <html:submit>Go</html:submit>
        </html:form>
        </div>
        </li>
        <li>
            <a href="/${WEB_PROPERTIES['webapp.path']}/templates.do" alt="" class="section_link">
            <div>
            <h3>Query Data</h3><br/>
                Use templates to query for particular subsets and combinations of data.<br>
                <img src="model/images/query_data.jpg" width="191" height="72" alt="Query Data">
            </div>
            </a>
        </li>
        <li class="last_section">
            <div>
            <h3>Search for Genes</h3><br/>
                Find modENCODE features
                <a href="/${WEB_PROPERTIES['webapp.path']}/template.do?name=gene_overlapping_flanking_regions" alt="">
                near a specific gene</a>.
                <br/>
                <br/>
                Or look up a Gene:
                <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get" style="display:inline;">

                    <input style="" id="quickSearchInput" name="searchTerm" type="text" class="qs_input">
                    <input type="submit" name="searchSubmit" value="GO" />
                </form>
                <br/>
                Or <html:link href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=upload">upload a list</html:link>.
            </div>
        </li>
    </ol>
    <br clear="both"/>
</div>
<script type="text/javascript" charset="utf-8">
    jQuery(document).ready(function(){
        jQuery(".frontpage_sections div").bg(['10px', '10px', '10px', '10px']);
    });
</script>
<%--
<table cellpadding="0" cellspacing="0" border="0">
<tr><td align="center"><b>modMine web services let you query modENCODE data directly
from Perl scripts <a href="http://blog.modencode.org/modmine-perl-api">more information and examples</a></b></td></tr>
</table>
<br/>
--%>
<!-- The projects section -->
<tiles:insert name="projectsSummary.tile" />
</div>
</div>
