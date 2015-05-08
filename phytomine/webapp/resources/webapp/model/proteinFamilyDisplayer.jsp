<!-- proteinFamilyDisplayer.jsp -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="proteinfamily_displayer" class="collection-table">

<h3> Protein Family Membership </h3>

<c:choose>
  <c:when test="${!empty list}">
    <div>
    This protein is a member of ${fn:length(list)} families
    <table>
      <thead>
       <tr>
         <th> Node Name </th>
         <th> Cluster ID </th>
         <th> Cluster Name </th>
         <th> Member Count </th>
         <th> Membership Detail </th>
         <th> MSA </th>
       </tr>
    </thead>
    <tbody>
      <c:forEach var="row" items="${list}">
        <tr>
           <td> ${row.methodName} </td>
           <td> <a href="report.do?id=${row.familyId}"> ${row.clusterId} </a> </td>
           <td> ${row.clusterName} </td>
           <td> ${row.memberCount} </td>
           <td> ${row.membershipDetail} </td>
           <c:choose>
             <c:when test="${!empty row.msaId}">
               <td> <a href="report.do?id=${row.msaId}"> MSA </a> </td>
             </c:when>
             <c:otherwise>
               <td> Singleton Cluster - No MSA </td>
             </c:otherwise>
           </c:choose>
        </tr>
      </c:forEach>
    </tbody>
    </table>

    </div>
   </div>
  </c:when>
  <c:otherwise>
    This protein is not a member of any family
  </c:otherwise>
</c:choose>


<!-- /proteinFamilyDisplayer.jsp -->
