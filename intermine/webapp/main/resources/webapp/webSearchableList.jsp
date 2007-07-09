<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- webSearchableList.jsp -->

<tiles:importAttribute name="type"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>
<tiles:importAttribute name="makeTable" ignore="true"/>
<tiles:importAttribute name="tableHeader" ignore="true"/>
<tiles:importAttribute name="tableRow" ignore="true"/>

<html:xhtml/>
<c:choose>
  <c:when test="${empty filteredWebSearchables}">
    <div class="altmessage">
      [nothing to display]
    </div>
  </c:when>
  <c:otherwise>
    <div class="wsList">
      <c:choose>
        <c:when test="${!empty makeTable && makeTable}">
          <%-- make a table --%>
          <table>
            <tr>
              <tiles:insert name="${tableHeader}">
                <tiles:put name="scope" value="${scope}"/>
                <tiles:put name="tags" value="${tags}"/>
                <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
              </tiles:insert>
            </tr>
            <c:forEach items="${filteredWebSearchables}" var="entry">
              <c:set var="webSearchable" value="${entry.value}"/>
              <tr class="${ageClasses[entry.key]}">
                <tiles:insert name="${tableRow}">
                  <tiles:put name="wsName" value="${entry.key}"/>
                  <tiles:put name="webSearchable" beanName="webSearchable"/>
                  <tiles:put name="statusIndex" value="${status.index}"/>
                  <tiles:put name="wsCheckBoxId" value="selected_${scope}_${type}_${status.index}"/>
                  <tiles:put name="scope" value="${scope}"/>
                  <tiles:put name="tags" value="${tags}"/>
                </tiles:insert>
              </tr>
            </c:forEach>
          </table>
        </c:when>
        <c:otherwise>
          <%-- make a list --%>
          <ul>
            <c:forEach items="${filteredWebSearchables}" var="entry">
              <li/>
              <div class="wsListElement">
                <html:link action="/gotows?type=${type}&amp;scope=${scope}&amp;name=${entry.key}">${entry.value.title}
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
                  <tiles:put name="templateName" value="${entry.value.title}"/>
                  <tiles:put name="makeCheckBoxes" value="${makeCheckBoxes}"/>
                </tiles:insert>
              </div>
              <c:if test="${showDescriptions}">
                <div class="wsListDescription">
                  ${entry.value.description}
                </div>
              </c:if>
            </c:forEach>
          </ul>
        </c:otherwise>
      </c:choose>
    </div>
  </c:otherwise>
</c:choose>
<!-- /webSearchableList.jsp -->
