<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute name="displayObject"/>
<tiles:importAttribute name="aspect"/>

<!-- objectDetailsAspect.jsp -->

<im:heading id="${category}">
  ${aspect}<%--<im:helplink key="objectDetails.help.otherInfo"/>--%>
</im:heading>
<im:body id="${category}">
  <tiles:insert page="/objectDetailsAspectRefsCols.jsp">
    <tiles:put name="object" beanName="displayObject"/>
    <tiles:put name="aspect" value="${aspect}"/>
  </tiles:insert>
  <tiles:insert name="templateList.tile">
    <tiles:put name="type" value="global"/>
    <tiles:put name="aspect" value="${aspect}"/>
    <tiles:put name="displayObject" beanName="displayObject"/>
    <tiles:put name="noTemplatesMsgKey" value="templateList.noTemplates"/>
  </tiles:insert>
  <tiles:insert page="/objectDetailsDisplayers.jsp">
    <tiles:put name="aspect" value="${aspect}"/>
    <tiles:put name="displayObject" beanName="displayObject"/>
  </tiles:insert>
  <im:vspacer height="5"/>
</im:body>

<!-- /objectDetailsAspect.jsp -->

