<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- webSearchableList.jsp -->

<tiles:importAttribute name="type"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>

<html:xhtml/>
<div class="webSearchableList">
<ul>
  <c:forEach items="${filteredWebSearchables}" var="webSearchableEntry">
<li/>
    <div class="webSearchableListElement">
    <html:link action="/gotows?type=${type}&amp;scope=${scope}&amp;name=${webSearchableEntry.key}">${webSearchableEntry.value.title}
    <c:choose>
    <c:when test="${type=='template'}">
    <img border="0" class="arrow" src="images/template_t.gif" alt="-&gt;"/>
    </c:when>
    <c:otherwise>
        <img border="0" class="arrow" src="images/bag_ico.gif" alt="-&gt;"/>
	</c:otherwise>
	</c:choose>
    </html:link>
  <tiles:insert name="starTemplate.tile" flush="false">
    <tiles:put name="templateName" value="${webSearchableEntry.value.title}"/>
  </tiles:insert>
   </div>
     <c:if test="${showDescriptions}">
      <div class="webSearchableListDescription">
        ${webSearchableEntry.value.description}
      </div>
    </c:if>
  </c:forEach>
<ul>
</div>

<!-- /webSearchableList.jsp -->
