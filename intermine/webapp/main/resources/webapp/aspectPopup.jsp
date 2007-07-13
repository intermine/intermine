
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- aspectPopup.jsp -->

<html:xhtml/>

<script type="text/javascript">
  <!--//<![CDATA[
    function changeaspect() {
      var url = '${requestScope['javax.servlet.include.context_path']}/aspect.do?';
      var set = $('aspectSelector').options[$('aspectSelector').selectedIndex].value;
      if (set != '') {
        url += 'name=' + set;
        document.location.href=url;
      }
    }
    //]]>-->
</script>

<form action="<html:rewrite action="/aspect"/>" name="aspectForm">
  <%-- Page size controls --%>
  <fmt:message key="aspect.starting.point"/>
  <select name="name" onchange="changeaspect()" id="aspectSelector" style="font-size:11px">
    <c:if test="${aspect == null}">
      <option value="" selected>-- Choose aspect --</option>
    </c:if>
    <c:forEach items="${ASPECTS}" var="entry">
      <c:set var="set" value="${entry.value}"/>
      <option value="${set.name}"
        <c:if test="${aspect.name == set.name}">
          selected
        </c:if>
      >${set.name}</option>
    </c:forEach>
  </select>
  <noscript>
    <input type="submit" value="<fmt:message key="button.change"/>" style="font-size:11px" />
  </noscript>
</form>

<!-- /aspectPopup.jsp -->
