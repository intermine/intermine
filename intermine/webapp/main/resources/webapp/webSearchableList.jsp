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
  <c:forEach items="${filteredWebSearchables}" var="webSearchableEntry">
    <html:link action="/gotows?type=${type}&amp;scope=${scope}&amp;name=${webSearchableEntry.key}">
      <div class="webSearchableListElement">
        ${webSearchableEntry.value.title}  
        
  <html:link action="/template?name=${webSearchableEntry.value.title}&amp;scope=global" 
             title="${webSearchableEntry.value.title}">
    <img border="0" class="arrow" src="images/template_t.gif" alt="-&gt;"/>
  </html:link>
        
        			<tiles:insert name="starTemplate.tile">
                      <tiles:put name="templateName" value="${webSearchableEntry.value.title}"/>
                    </tiles:insert>
              
      </div>
      </html:link>
     <c:if test="${showDescriptions}">
      <div class="webSearchableListDescription">
        ${webSearchableEntry.value.description}        
      </div>
     </c:if>
    
  </c:forEach>
</div>

<!-- /webSearchableList.jsp -->
