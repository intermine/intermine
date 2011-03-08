<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- customDisplayer.jsp -->
    <tiles:importAttribute name="displayer"/>

<%--<c:out value="jspPage: ${jspPage}"/> <c:out value="type: ${type}"/><br/>--%>

    <jsp:include page="${jspPage}"/>

<!-- /customDisplayer.jsp -->
