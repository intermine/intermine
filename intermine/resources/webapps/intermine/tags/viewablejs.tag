<%@ tag body-content="empty" %>
<%@ attribute name="idPrefixes" required="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- JavaScript functions required once on the page before using <im:viewable ...> --%>

<c:set var="prefixes" value="${fn:split(idPrefixes, ',')}"/>

<script type="text/javascript">
<!--

function enterPath(path)
{
  setBorderStyle(path, "#fff");
}

function exitPath(path)
{
  setBorderStyle(path, "#f5f0ff");
}

function setBorderStyle(path, style)
{
  var a = null;
  <c:forEach var="prefix" items="${prefixes}" varStatus="status">
    a = document.getElementById("${prefix}"+path);
    if (a != null) {
      a.style.background = style;
    }
  </c:forEach>
}

//-->
</script>


