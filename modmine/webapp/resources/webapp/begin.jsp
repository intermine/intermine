<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml />

<div class="body">

<table>
	<tr>
		<td colspan=2>
		<im:boxarea title="Projects"
			titleLink="/${WEB_PROPERTIES['webapp.path']}/projects.do"
			stylename="plainbox">
			<tiles:insert name="projectsSummary.tile" />
		</im:boxarea>
</td>
</tr>

	<tr valign=top>
<%--
		<td>
    <im:boxarea title="Latest Submissions"
      stylename="plainbox">
      <tiles:insert name="latestSubs.tile" />
    </im:boxarea> 
		</td>
--%>

		<!-- Second column -->
		<td>

    <im:boxarea title="Lists"
      titleLink="/${WEB_PROPERTIES['webapp.path']}/bag.do"
      stylename="gradientbox">
      <p><em><fmt:message key="begin.bags" /></em></p>
      <br />
      <div>Example lists (<a
        href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view">${bagCount}
      total</a>):</div>
      <div id="bagsList" class="frontBoxList"><tiles:insert
        name="webSearchableList.tile">
        <tiles:put name="limit" value="2" />
        <tiles:put name="wsListId" value="all_bag" />
        <!-- bag or template? -->
        <tiles:put name="type" value="bag" />
        <!-- user or global -->
        <tiles:put name="scope" value="all" />
        <tiles:put name="tags" value="im:frontpage" />
        <tiles:put name="showSearchBox" value="false" />
        <tiles:put name="showCount" value="true" />
      </tiles:insert></div>
      <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view"
        styleClass="fp_button">
        <img src="theme/view_lists.png" id="view_lists"
          title="Click here to View Lists" height="22px" width="115px" />
      </html:link>
    <br clear="right" /> 
      <html:link
        href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=upload"
        styleClass="fp_button">
        <img src="/theme/create_lists.png" id="create_lists"
          title="Click here to Upload Lists" height="22px" width="120px" />
      </html:link>
    <br clear="right" />
    </im:boxarea>
<td>
		<im:boxarea title="Templates"
			titleLink="/${WEB_PROPERTIES['webapp.path']}/templates.do"
			stylename="gradientbox">
			<em>
			<p><fmt:message key="begin.templates" /></p>
			</em>
			<br />
			<div>Example templates (<a
				href="/${WEB_PROPERTIES['webapp.path']}/templates.do">${templateCount}
			total</a>):</div>
			<div id="templatesList" class="frontBoxList"><tiles:insert
				name="webSearchableList.tile">
				<!-- optional -->
				<tiles:put name="limit" value="3" />
				<!-- bag or template? -->
				<tiles:put name="type" value="template" />
				<!-- user or global -->
				<tiles:put name="wsListId" value="all_template" />
				<tiles:put name="scope" value="all" />
				<tiles:put name="tags" value="im:frontpage" />
				<tiles:put name="showDescriptions" value="false" />
				<tiles:put name="showSearchBox" value="false" />
				<tiles:put name="showCount" value="false" />
			</tiles:insert></div>
			<html:link href="/${WEB_PROPERTIES['webapp.path']}/templates.do"
				styleClass="fp_button">
				<img src="theme/search_with_templates.png"
					id="search_with_templates"
					title="Click here to Search using Template Queries" height="22px"
					width="153px" />
			</html:link>
			<br clear="right" />
		</im:boxarea> 

		
    </td>
		
		
	</tr>
</table>


</div>
