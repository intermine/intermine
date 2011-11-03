<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- queryBuilderPage.jsp -->
<im:viewablejs idPrefixes="nav,browser,query,showing"/>

<tiles:get name="queryBuilder"/>
<tiles:insert page="templateSettings.jsp"/>
<tiles:insert page="queryBuilderActions.jsp"/>
<!-- /queryBuilderPage.jsp -->
