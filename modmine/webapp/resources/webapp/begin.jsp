<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml />

<div class="body">

<link rel="stylesheet" href="model/css/frontpage_sections.css" type="text/css" media="screen" title="no title" charset="utf-8">
<!-- Entry point section -->
<div align="center">
<div class="frontpage_sections">
    <ol>
        <li>
            <a href="/${WEB_PROPERTIES['webapp.path']}/begin.do#projects" alt="" class="section_link">
            <div>
            <h3>Get Data</h3><br/>
                Click an experiment below to export data, view GBrowse tracks and analyse lists.<br>
                <img src="model/images/get_data.jpg" width="63" height="62" alt="Get Data" style="align:middle">
            </div>
            </a>
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
                Find modENCODE data related to a particular gene.  Search for a Gene:
                <html:form action="/quickSearchAction">
                    <input id="quickSearchType" name="quickSearchType" type="hidden" value="ids">
                    <input style="" id="quickSearchInput" name="value" type="text" class="qs_input">
                    <html:submit><fmt:message key="header.search.button"/></html:submit>
                </html:form>
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

<!-- The projects section -->
<tiles:insert name="projectsSummary.tile" />
</div>
</div>
