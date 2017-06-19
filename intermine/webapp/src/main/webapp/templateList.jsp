<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- templateList.jsp -->

<html:xhtml/>

<tiles:importAttribute name="reportObject" ignore="true"/>
<tiles:importAttribute name="interMineIdBag" ignore="true"/>
<tiles:importAttribute name="noTemplatesMsgKey" ignore="true"/>
<tiles:importAttribute name="placement"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="trail" ignore="true"/>

<c:if test="${!empty reportObject}">
  <c:set var="interMineObject" value="${reportObject.object}"/>
</c:if>
<c:forEach items="${templates}" var="templateQuery" varStatus="status">
  <tiles:insert name="reportTemplate.jsp">
    <tiles:put name="reportObject" beanName="reportObject"/>
    <tiles:put name="interMineIdBag" beanName="interMineIdBag"/>
    <tiles:put name="templateQuery" beanName="templateQuery"/>
    <tiles:put name="placement" value="${placement}"/>
    <tiles:put name="scope" value="${scope}"/>
    <tiles:put name="trail" value="${trail}"/>
  </tiles:insert>
</c:forEach>

<!-- /templateList.jsp -->
