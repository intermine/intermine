<%@ tag body-content="empty" %>
<%@ attribute name="idPrefixes" required="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- JavaScript functions required once on the page before using <im:viewable ...>
	 and <im:sortable ...>
     The prefixes are the set of values later passed as the idPrefix attribute
     when using <im:viewable>
--%>

<c:set var="prefixes" value="${fn:split(idPrefixes, ',')}"/>

<script type="text/javascript">
<!--

var highlightingEnabled = true;

function enterPath(path)
{
  if (!highlightingEnabled)
    return;
  setStyle(path, 1);
}

function exitPath(path)
{
  setStyle(path, 0);
}

function setStyle(path, inside)
{
  var a = null;
  <c:forEach var="prefix" items="${prefixes}" varStatus="status">
    a = document.getElementById("${prefix}" + path);
    if (a != null) {
      if (inside) {
        a.style.borderColor = "#444";
      } else {
        a.style.borderColor = "#BBB";
      }
    } else {
      a = document.getElementsByName("${prefix}" + path);
      if (a != null && a.length > 0) {
        if (inside) {
          a[0].style.borderColor = "#444";
        } else {
          a[0].style.borderColor = "#BBB";
        }
      }
    }
  </c:forEach>
}

function disblePathHighlighting() {
  highlightingEnabled = false;
}

function enablePathHighlighting() {
  highlightingEnabled = true;
}

/* Install document mouse up handler to reenable highlighting */

Event.observe(document, "mouseup", enablePathHighlighting);

//-->
</script>


