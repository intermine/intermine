<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- api.jsp -->
<html:xhtml/>

<div class="body">

  <c:choose>
    <c:when test="${empty subtabs[subtabName]  || subtabs[subtabName] == 'perl'}">
        <div align="center" style="padding-top: 20px;">
          <tiles:insert name="apiPerl.tile"/>
        </div>
    </c:when>
    <c:when test="${subtabs[subtabName] == 'python'}">
        <div align="center" style="padding-top: 20px;">
          <tiles:insert name="apiPython.tile"/>
        </div>
    </c:when>
    <c:when test="${subtabs[subtabName] == 'ruby'}">
        <div align="center" style="padding-top: 20px;">
          <tiles:insert name="apiRuby.tile"/>
        </div>
    </c:when>
    <c:otherwise>
          <div align="center" style="padding-top: 20px;">
          <tiles:insert name="apiJava.tile"/>
          </div>
    </c:otherwise>
  </c:choose>

</div>
<!-- /api.jsp -->