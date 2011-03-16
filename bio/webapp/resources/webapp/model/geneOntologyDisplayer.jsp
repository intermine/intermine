<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- geneOntologyDisplayer.jsp -->
<h3>Gene Ontology</h3>

<table>
<tr>
    <th>main ontology</th>
    <th>go term</th>
    <th>evidence code</th>
</tr>
<c:forEach items="${goTerms}" var="parentEntry">
<c:set var="parentTerm" value="${parentEntry.key}" />
<c:forEach items="${parentEntry.value}" var="entry">
    <c:set var="term" value="${entry.key}" />
    <c:set var="evidence" value="${entry.value}" />
<tr>
    <td>
         <c:out value="${parentTerm}"/>
    </td>
    <td>
<c:out value="${term}"/>
    </td>
    <td>

<c:forEach items="${entry.value}" var="evidence">
    <c:out value="${evidence}"/>&nbsp;
</c:forEach>
    </td>
</tr>
</c:forEach>
</c:forEach>
</table>

<!-- /geneOntologyDisplayer.jsp -->
