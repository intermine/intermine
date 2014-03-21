<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- dataCategories -->
<html:xhtml/>

<div class="body">
  <div id="leftCol">
      <div id="pageDesc" class="pageDesc"><p><fmt:message key="dataCategories.intro"/></p></div>
      <im:boxarea title="Actions" stylename="plainbox" >
           <html:link action="/templates">
             <fmt:message key="dataCategories.viewTemplates"/>
             <img border="0" class="arrow" src="images/right-arrow.gif" alt="Go"/>
           </html:link>
      </im:boxarea>
   </div>
<div id="rightCol">
   <im:boxarea titleKey="dataCategories.title" stylename="gradientbox">
    <c:choose>
        <c:when test="${!empty ASPECTS}">
           <tiles:insert name="aspects.tile"/>
        </c:when>
        <c:otherwise>
          <c:forEach items="${CATEGORIES}" var="category">
            <c:if test="${!empty CATEGORY_CLASSES[category]}">
              <div class="heading"><c:out value="${category}"/></div>
              <div class="body">
                <c:set var="classes" value="${CATEGORY_CLASSES[category]}"/>
                <c:forEach items="${classes}" var="classname" varStatus="status">
                  <a href="<html:rewrite page="/queryClassSelect.do"/>?action=<fmt:message key="button.selectClass"/>&amp;className=${classname}" title="<c:out value="${classDescriptions[classname]}"/>">
                    ${classname}</a><c:if test="${!status.last}">,</c:if>
                </c:forEach>
                <c:if test="${!empty CATEGORY_TEMPLATES[category]}">
                  <br/><span class="smallnote"><fmt:message key="begin.or"/> <html:link action="/templates" paramId="category" paramName="category"><fmt:message key="begin.related.templates"/></html:link></span>
                </c:if>
              </div>
              <im:vspacer height="5"/>
            </c:if>
          </c:forEach>
        </c:otherwise>
      </c:choose>
     </im:boxarea>
</div>
</div>
</div>
<!-- /dataCategories -->
