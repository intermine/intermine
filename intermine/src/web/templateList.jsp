<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- templatelist.jsp -->

<tiles:useAttribute id="templates" name="templates" classname="java.util.Map" ignore="true"/>
<tiles:importAttribute name="headingKey"/>
<tiles:importAttribute name="templateType"/>
<tiles:importAttribute name="showDelete" ignore="true"/>

<c:if test="${!empty templates}">
  <table class="box" cellspacing="0" cellpadding="6" border="0" width="100%" align="center">
    <tr>
      <th align="left" class="title"><fmt:message key="${headingKey}"/></th>
      <th align="right" class="help">
        [<html:link href="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/manualStartingaquery.html">
          <fmt:message key="begin.link.help"/>
        </html:link>]
      </th>
    </tr>
    <tr>
      <td>
        <c:forEach items="${templates}" var="templateQuery" varStatus="status">
          <span class="templateDesc"><c:out value="${templateQuery.value.description}"/></span>&nbsp;
          <html:link action="/template?name=${templateQuery.key}&type=${templateType}">
            <img border="0" class="arrow" src="images/right-arrow.png" alt="->"/>
          </html:link>
          <c:if test="${showDelete == 1}">
            <html:link action="/userTemplateAction?method=delete&name=${templateQuery.key}&type=${templateType}">
              <img border="0" src="images/cross.png" alt="x"/>
            </html:link>
          </c:if>
          <br/>
        </c:forEach>
      </td>
    </tr>
  </table>
</c:if>

<!-- /templatelist.jsp -->
