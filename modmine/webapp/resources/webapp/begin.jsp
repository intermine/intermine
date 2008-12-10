<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>

<div class="body">
     <im:boxarea title="Projects" titleLink="/${WEB_PROPERTIES['webapp.path']}/projects.do" stylename="plainbox" >
         <tiles:insert name="projectsSummary.tile"/>
    </im:boxarea>

<div id="leftcolumn">
     <im:boxarea title="Submissions" titleLink="/${WEB_PROPERTIES['webapp.path']}/submissions.do" stylename="plainbox" fixedWidth="300px">
         <tiles:insert name="latestSubs.tile"/>
    </im:boxarea>

      <im:boxarea title="Lists" titleLink="/${WEB_PROPERTIES['webapp.path']}/bag.do" stylename="gradientbox" fixedWidth="300px">
        <p><em><fmt:message key="begin.bags"/></em></p>
        <br/>
        <div>
          Example lists (<a href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view">${bagCount} total</a>):
        </div>
        <div id="bagsList" class="frontBoxList">
        <tiles:insert name="webSearchableList.tile">
          <tiles:put name="limit" value="2"/>
          <tiles:put name="wsListId" value="all_bag"/>
          <%-- bag or template? --%>
          <tiles:put name="type" value="bag"/>
          <%-- user or global --%>
          <tiles:put name="scope" value="all"/>
          <tiles:put name="tags" value="im:frontpage"/>
          <tiles:put name="showSearchBox" value="false"/>
          <tiles:put name="showCount" value="true"/>
          <%--<tiles:put name="height" value="100"/>--%>
        </tiles:insert>
        </div>
        <html:link href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=view" styleClass="fp_button"><img src="theme/view_lists.png" id="view_lists" title="Click here to View Lists" height="22px" width="115px" /></html:link><br clear="right"/>
        <html:link href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=upload" styleClass="fp_button"><img src="/theme/create_lists.png" id="create_lists" title="Click here to Upload Lists" height="22px" width="120px" /></html:link><br clear="right"/>
      </im:boxarea>



<!-- First column -->
<%--


     <im:boxarea title="Projects" titleLink="/${WEB_PROPERTIES['webapp.path']}/projects.do" stylename="plainbox" fixedWidth="300px">
     <em><p><fmt:message key="projects.intro"/></p></em>     
     <tiles:insert name="projectList.tile"/>
    </im:boxarea>


    <im:boxarea title="Data Categories" titleLink="/${WEB_PROPERTIES['webapp.path']}/dataCategories.do" stylename="plainbox" floatValue="left" fixedWidth="300px">
     <em><p><fmt:message key="begin.data"/></p></em>
     <c:set var="numPerCol" value="${fn:length(ASPECTS)/2}"/>
          <table cellpadding="0" cellspacing="0" border="0"><tr>
         <c:forEach var="entry" items="${ASPECTS}" varStatus="status">
           <c:set var="set" value="${entry.value}"/>
           <c:if test="${status.count%2 == '1'}"></tr><tr></c:if>
                   <td style="height:80px;padding:4px">
                     <a href="/${WEB_PROPERTIES['webapp.path']}/aspect.do?name=${set.name}">
                       <img src="<html:rewrite page="/${set.iconImage}"/>" class="aspectIcon"
                            title="Click here to view the ${set.name} Data Category"
                            width="40px" height="40px" />
                     </a>
                   </td>
                   <td>
                     <a href="/${WEB_PROPERTIES['webapp.path']}/aspect.do?name=${set.name}">
                       ${set.name}
                     </a>
                   </td>
             </c:forEach>
          </tr></table>
    </im:boxarea>
--%>

</div>
<div id="rightcolumn">
<!-- Second column - elastic -->
<%-- 
      <div>
        <c:if test="${!empty WEB_PROPERTIES['project.rss']}">
          <tiles:insert name="news.tile" />
        </c:if>
      </div>
--%>

    <im:boxarea title="Templates" titleLink="/${WEB_PROPERTIES['webapp.path']}/templates.do" stylename="gradientbox">
        <em><p><fmt:message key="begin.templates"/></p></em>
        <br/>
        <div>
          Example templates (<a href="/${WEB_PROPERTIES['webapp.path']}/templates.do">${templateCount} total</a>):
        </div>
        <div id="templatesList" class="frontBoxList">
          <tiles:insert name="webSearchableList.tile">
            <!-- optional -->
            <tiles:put name="limit" value="3"/>
            <!-- bag or template? -->
            <tiles:put name="type" value="template"/>
            <!-- user or global -->
            <tiles:put name="wsListId" value="all_template"/>
            <tiles:put name="scope" value="all"/>
            <tiles:put name="tags" value="im:frontpage"/>
            <tiles:put name="showDescriptions" value="false"/>
            <tiles:put name="showSearchBox" value="false"/>
            <tiles:put name="showCount" value="false"/>
          </tiles:insert>
        </div>

        <html:link href="/${WEB_PROPERTIES['webapp.path']}/templates.do" styleClass="fp_button"><img src="theme/search_with_templates.png" id="search_with_templates" title="Click here to Search using Template Queries" height="22px" width="153px" /></html:link><br clear="right"/>
      </im:boxarea>
      <im:boxarea title="Query Builder" titleLink="/${WEB_PROPERTIES['webapp.path']}/customQuery.do" stylename="gradientbox">
        <p><em><fmt:message key="begin.querybuilder"/></em></p>
        <br/>
        <div>
          <div id="qbStartQuery">
            Start a query from:
            <!-- loop through starting classes -->
            <c:forEach var="entry" items="${WEB_PROPERTIES['begin.query.classes']}" varStatus="status"><c:if test="${status.count != 1}">,</c:if>&nbsp;<a href="/${WEB_PROPERTIES['webapp.path']}/queryClassSelect.do?action=Select&amp;className=${entry}" rel="NOFOLLOW">${entry}</a></c:forEach>
          </div>
        </div>
        <div id="qbImport">
          <a href="/${WEB_PROPERTIES['webapp.path']}/importQueries.do?query_builder=yes">
            <fmt:message key="begin.importQuery"/>
          </a>
        </div>
        <html:link href="/${WEB_PROPERTIES['webapp.path']}/customQuery.do" styleClass="fp_button"><img src="/theme/build_a_query.png" id="build_a_query" title="Click here to Build A Query" height="22px" width="120px" /></html:link><br clear="right"/>
      </im:boxarea>



</div>



</div>

<!-- /begin.jsp -->
