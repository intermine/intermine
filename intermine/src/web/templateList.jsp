<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- templatelist.jsp -->

<tiles:useAttribute id="templates" name="templates" classname="java.util.Map" ignore="true"/>
<tiles:importAttribute name="headingKey"/>
<tiles:importAttribute name="templateType"/>
<tiles:importAttribute name="showDelete" ignore="true"/>
<tiles:importAttribute name="showEdit" ignore="true"/>

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
            <%-- pull required messages --%>
            <fmt:message var="confirmMessage" key="templateList.deleteMessage">
             <fmt:param value="${templateQuery.key}"/>
            </fmt:message>
            <fmt:message var="linkTitle" key="templateList.delete">
              <fmt:param value="${templateQuery.key}"/>
            </fmt:message>
            <%-- map of parameters to pass to the confirm action --%>
            <jsp:useBean id="deleteParams" scope="page" class="java.util.TreeMap">
              <c:set target="${deleteParams}" property="message" value="${confirmMessage}" />
              <c:set target="${deleteParams}" property="confirmAction" value="/userTemplateAction?method=delete&name=${templateQuery.key}&type=${templateType}" />
              <c:set target="${deleteParams}" property="cancelAction" value="/begin" />
            </jsp:useBean>
            <html:link action="/confirm" name="deleteParams" title="${linkTitle}">
              <img border="0" src="images/cross.png" alt="x"/>
            </html:link>
          </c:if>
          <c:if test="${showEdit == 1}">
            <html:link action="/editTemplate?name=${templateQuery.key}">
              <img border="0" class="arrow" src="images/edit.png" alt="->"/>
            </html:link>
          </c:if>
          <c:if test="${!status.last}">
            <br/><img border="0" height="8" width="5" src="images/blank.png"/><br/>
          </c:if>
        </c:forEach>
      </td>
    </tr>
  </table>
</c:if>

<!-- /templatelist.jsp -->
