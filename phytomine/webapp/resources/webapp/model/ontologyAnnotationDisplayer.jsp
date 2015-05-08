<!-- OntologyDisplayer.jsp -->
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<div id="ontology_annotation_displayer" class="collection-table">

<h3>Ontology Annotations</h3>

<c:choose>
  <c:when test="${!empty list}">
    <div>
    There are ${fn:length(list)} ontology annotations.
    <c:set var="ontologyTerms" value="" />
    <table>
      <thead>
       <tr>
         <th>Ontology</th>
         <th>Term</th>
         <th>Name</th>
         <th>Namespace</th>
         <th>Description</th>
       </tr>
    </thead>
    <tbody>
      <c:forEach var="row" items="${list}">
        <tr>
           <td> ${row.ontology} </td>
           <td> ${row.term} </td>
           <td> ${row.name} </td>
           <td> ${row.namespace} </td>
           <td> ${row.description} </td>
         <c:set var="ontologyTerms" value="${ontologyTerms} ${row.term}" />
        </tr>
      </c:forEach>
    </tbody>
    </table>

    </div>
   </div>
  </c:when>
  <c:otherwise>
    No Ontology data available
  </c:otherwise>
</c:choose>


<script type="text/javascript">
        numberOfTableRowsToShow=100000
        trimTable('#ontology_annotation_displayer');
</script>


</div>
