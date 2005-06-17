
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- dataSetPopup.jsp -->

<html:xhtml/>

<script type="text/javascript">
  <!--//<![CDATA[
    function changeDataSet() {
      var url = '${requestScope['javax.servlet.include.context_path']}/dataSet.do?';
      var set = document.dataSetForm.name.options[document.dataSetForm.name.selectedIndex].value;
      url += 'name=' + set;
      document.location.href=url;
    }
    //]]>-->
</script>

<form action="<html:rewrite action="/dataSet"/>" name="dataSetForm">
        <%-- Page size controls --%>
        <fmt:message key="dataset.starting.point"/>
        <select name="name" onchange="changeDataSet()" class="dataSetSelect">
          <c:forEach items="${DATASETS}" var="entry">
            <c:set var="set" value="${entry.value}"/>
            <option value="${set.name}"
              <c:if test="${dataSet.name == set.name}">
                selected
              </c:if>
            >${set.name}</option>
          </c:forEach>
        </select>
        <noscript>
          <input type="submit" value="<fmt:message key="button.change"/>" class="dataSetSelect"/>
        </noscript>
</form>

<!-- /dataSetPopup.jsp -->
