<%@ tag body-content="scriptless" %>
<%@ attribute name="src" required="true" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="link" required="true" %>
<%@ attribute name="height" required="true" %>
<%@ attribute name="width" required="true" %>
<%@ attribute name="floatValue" required="false" %>
<%@ attribute name="breakFloat" required="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<c:set var="iePre7" value='<%= new Boolean(request.getHeader("user-agent").matches(".*MSIE [123456].*")) %>'/>

<c:if test="${! empty floatValue}">
  <c:set var="floatValue" value="left"/>
</c:if>

<c:choose>
  <c:when test="${iePre7}">
    <style type="text/css">
        div.${id} {
        background:none;
        height:${height}; 
        width:${width};
        cursor:pointer;
        filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(src='${src}' ,sizingMethod='crop');
      }
    </style>
  </c:when>
  <c:otherwise>
    <style type="text/css">
      div.${id} {
        background:url('${src}') no-repeat;
        height:${height};
        width:${width};
        cursor:pointer;
      }
    </style>
  </c:otherwise>
</c:choose>

<html:link action="${link}" >
  <div class="${id}" style="float:${floatValue}">&nbsp</div>
</html:link>
<c:if test="${! empty breakFloat && breakFloat == 'true'}">
  <div class="clear-both"></div>
</c:if>