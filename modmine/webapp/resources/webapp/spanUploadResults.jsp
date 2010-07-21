<%--
  - Author: Fengyuan Hu
  - Date: 11-06-2010
  - Copyright Notice:
  - @(#)  Copyright (C) 2002-2010 FlyMine

          This code may be freely distributed and modified under the
          terms of the GNU Lesser General Public Licence.  This should
          be distributed with the code.  See the LICENSE file for more
          information or http://www.gnu.org/copyleft/lesser.html.

  - Description: In this page, it displays the results of overlapping
                 located sequence features with the constrains and spans
                 by users'setup
  --%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!--  spanUploadResults.jsp -->

<html:xhtml />

<script language="javascript">

</script>

<p align="center"><h1>Result</h1></p>
<div>

<table cellspacing="0" cellpadding="0" border="0" align="left" >
<tr>
  <td valign="top">
  <table cellspacing="1" cellpadding="1" border="1" width="100%">

    <tr valign="top">
      <th>Span</th>
      <th>Feature PID</th>
      <th>Feature Type</th>
      <th>Chromosome</th>
      <th>Start</th>
      <th>End</th>
      <th>Submission DCCid</th>
    </tr>

    <c:forEach var="element" items="${results}">

    <tr>
      <td rowspan="${fn:length(element.value)}">
        <c:out value="${element.key}"/>
      </td>

      <c:forEach var="result" begin="0" end="0" items="${element.value}">
          <td><c:out value="${result[0]}"/></td>
          <td><c:out value="${result[1]}"/></td>
          <td><c:out value="${result[2]}"/></td>
          <td><c:out value="${result[3]}"/></td>
          <td><c:out value="${result[4]}"/></td>
          <td><c:out value="${result[5]}"/></td>
      </c:forEach>
    </tr>

    <c:forEach var="result" begin="1" end="${fn:length(element.value)-1}" items="${element.value}">
        <tr>
          <td><c:out value="${result[0]}"/></td>
          <td><c:out value="${result[1]}"/></td>
          <td><c:out value="${result[2]}"/></td>
          <td><c:out value="${result[3]}"/></td>
          <td><c:out value="${result[4]}"/></td>
          <td><c:out value="${result[5]}"/></td>
        </tr>
    </c:forEach>

    </c:forEach>

  </table>
  </td>
</tr>
</table>

</div>


<!--  /spanUploadResults.jsp -->