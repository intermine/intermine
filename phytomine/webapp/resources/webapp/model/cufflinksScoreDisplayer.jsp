<!-- cufflinksScoreDisplayer.jsp -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="expression_displayer" class="collection-table">

<h3> Expression Quantification </h3>

<c:choose>
  <c:when test="${!empty list}">
    <div>
    There are ${fn:length(list)} expression records
    <table>
      <thead>
       <tr>
         <th> Experiment Name </th>
         <th> Experiment Group  </th>
         <th> FPKM </th>
       </tr>
    </thead>
    <tbody>
      <c:forEach var="row" items="${list}">
        <tr>
           <td> ${row.experiment} </td>
           <td> ${row.group} </td>
           <td> ${row.fpkm} </td>
        </tr>
      </c:forEach>
    </tbody>
    </table>

    </div>
   </div>
  </c:when>
  <c:otherwise>
    No expression data available
  </c:otherwise>
</c:choose>


<!-- /cufflinksScoreDisplayer.jsp -->
