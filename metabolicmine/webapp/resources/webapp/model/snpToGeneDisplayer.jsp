<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<html:xhtml/>

  <div class="geneInformation">
    <h3 class="overlapping">SNPs to overlapping Genes within 10.0kb</h3>

    <table cellspacing="0">
      <tr>
        <th>Gene Primary Identifier</th>
        <th>Gene name</th>
        <th>Gene Symbol</th>
        <th>Distance</th>
      </tr>
      <c:forEach var="row" items="${list}" varStatus="status">
        <tr class="${status.count mod 2 == 0 ? 'odd' : 'even'}">
          <c:forEach var="column" items="${row}" varStatus="columnStatus">
            <%-- do not display the trailing distance identifier --%>
            <c:if test="${columnStatus.count < 5}">
              <td>${column}</td>
            </c:if>
          </c:forEach>
        </tr>
      </c:forEach>
    </table>
  </div>
