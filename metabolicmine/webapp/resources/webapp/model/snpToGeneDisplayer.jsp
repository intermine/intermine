<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>
<div class="geneInformation">
  <h3 class="overlapping">Close Genes</h3>

  <table cellspacing="0">
    <tr><th>Row</th></tr>
    <c:forEach var="type" items="${response}" varStatus="status">
    <tr class="${status.count mod 2 == 0 ? 'odd' : 'even'}">
      <td>${type}</td>
    </tr>
    </c:forEach>
  </table>
</div>