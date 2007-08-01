<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>
<link rel="stylesheet" type="text/css" href="css/begin.css"/>

      <div class="body">
         <div style="float:left;margin-left:20px">
        <span style="font-size:+2em;">
          <a href="what.xml">What is FlyMine?</a>
        </span>&nbsp;&nbsp;<span style="font-size:+1.4em">
          <a href="tour_1.html" target="_blank"  onclick="javascript:window.open('tour_1.html','_manual','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Take a tour!</a>
        </span>

        <p class="errors">
          This is release 8.0 of FlyMine.  See the <a href="release-notes.xml">release notes</a> to find out what's new.
        </p>
        </div>
      <div style="margin-left:65%;width:350px;">
        <tiles:insert name="tipWrapper.tile"/>
      </div>
      <div style="clear:both;"></div>

<div id="leftColumn">
      <im:roundbox title="Templates" stylename="welcome" height="350">
        <p>
          <u>
            <fmt:message key="begin.trytemplates"/>
          </u>
        </p>
        <tiles:insert name="webSearchableList.tile">
          <!-- optional -->
          <tiles:put name="limit" value="3"/>
          <!-- bag or template? -->
          <tiles:put name="type" value="template"/>
          <!-- user or global -->
          <tiles:put name="wsListId" value="global_template"/>
          <tiles:put name="scope" value="global"/>
          <tiles:put name="tags" value="im:public"/>
          <tiles:put name="showDescriptions" value="false"/>
          <tiles:put name="showSearchBox" value="false"/>
          <%--<tiles:put name="height" value="100"/>--%>
        </tiles:insert>
        <style>
        div.gototemplates {background:url('images/go_to_template_page.png') no-repeat; height:76px; width:385px}
        </style>
        
        <%-- <c:set var="iePre7" value='<%= new Boolean(request.getHeader("user-agent").matches(".*MSIE [123456].*")) %>'/> --%>
        <c:if test="${! empty iePre7}">
        <style type="text/css">
        div.gototemplates {
        background:none;
        filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(src=’images/go_to_template_page.png’ ,sizingMethod=’crop’);
        }
        </style>
        </c:if>

        <html:link action="/templates.do">
        <div class="gototemplates">&nbsp</div>
        </html:link>
      </im:roundbox>

      <im:roundbox title="Lists" stylename="welcome" height="350">
        <p>
          <em>
            <fmt:message key="begin.bags"/>
          </em>
        </p>
        <p>
          <u>
            <fmt:message key="begin.trybags"/>
          </u>
        </p>
        <tiles:insert name="webSearchableList.tile">
          <tiles:put name="limit" value="3"/>
          <tiles:put name="wsListId" value="global_bag"/>
          <%-- bag or template? --%>
          <tiles:put name="type" value="bag"/>
          <%-- user or global --%>
          <tiles:put name="scope" value="global"/>
          <tiles:put name="tags" value="im:public"/>
          <tiles:put name="showSearchBox" value="false"/>
          <%--<tiles:put name="height" value="100"/>--%>
        </tiles:insert>
        <p>
          <u>
            <fmt:message key="begin.createbags"/>
          </u>
        </p>
                <html:link action="/bag.do">
                  <img  src="images/go_to_list_page.png" align="center" alt="Go To Bag Page" border="0">
                </html:link>
      </im:roundbox>

      <im:roundbox title="Query Builder" stylename="welcome" height="180">
        <p>
          <em>
            <fmt:message key="begin.querybuilder"/>
          </em>
        </p>
          <html:link action="/customQuery.do">
            <img src="images/go_to_query_builder.png" alt="Go To Query Builder">
          </html:link>
      </im:roundbox>
</div>

<div id="rightColumn">
  <div id="aspectsFront" class="actionArea">
     <h1>Data</h1>
        <!-- <p>
          <em>
            <fmt:message key="begin.data"/>
          </em>
        </p> -->
        <c:choose>
		    <c:when test="${!empty ASPECTS}">
		    <c:set var="colItemLength" value="${fn:length(ASPECTS) / 2}"/>
          <div style="float:left;width:150px">
		       <c:forEach var="entry" items="${ASPECTS}" varStatus="status">
                <c:if test="${status.count == colItemLength}"></div><div style="margin-left:150px;width:150px;"></c:if>
                   <div class="aspectOverview">
                     <c:set var="set" value="${entry.value}"/>
                     <html:link action="/aspect?name=${set.name}">
                       <img src="<html:rewrite page="/${set.iconImage}"/>" class="aspectIcon" />
                     </html:link>
                     <p class="aspectLabel">
                       <html:link action="/aspect?name=${set.name}">
                         ${set.name}
                       </html:link><br>
                       ${set.subTitle}
                     </p>
                  </div>
             </c:forEach>
          </div>
          <div style="clear:both;"></div>
		    </c:when>
		    <c:otherwise>
		      <c:forEach items="${CATEGORIES}" var="category">
		        <c:if test="${!empty CATEGORY_CLASSES[category]}">
		          <div class="heading"><c:out value="${category}"/></div>
		            <c:set var="classes" value="${CATEGORY_CLASSES[category]}"/>
		            <c:forEach items="${classes}" var="classname" varStatus="status">
		              <a href="<html:rewrite page="/queryClassSelect.do"/>?action=<fmt:message key="button.selectClass"/>&amp;className=${classname}" title="<c:out value="${classDescriptions[classname]}"/>">
		                ${classname}</a><c:if test="${!status.last}">,</c:if>
		            </c:forEach>
		            <c:if test="${!empty CATEGORY_TEMPLATES[category]}">
		              <br/><span class="smallnote"><fmt:message key="begin.or"/> <html:link action="/templates" paramId="category" paramName="category"><fmt:message key="begin.related.templates"/></html:link></span>
		            </c:if>
		          <im:vspacer height="5"/>
		        </c:if>
		      </c:forEach>
		    </c:otherwise>
		  </c:choose>
  </div>
</div>
</div>

<!-- /begin.jsp -->
