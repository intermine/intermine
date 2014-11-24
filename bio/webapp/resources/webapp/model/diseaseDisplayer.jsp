<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- diseaseDisplayer.jsp -->

<div id="mine-rat-disease" class="collection-table">

<c:choose>
  <c:when test="${ratGenes != null && !empty(ratGenes)}">
    <h3 class="loading">Diseases (from RatMine)</h3>

    <table>
        <tbody>
            <tr>
                <td>
                    <div class="intermine_rat_disease"></div>
                </td>
            </tr>
        </tbody>
    </table>

    <link type="text/css" rel="stylesheet" href="model/css/rat-diseases.css">
    <script type="text/javascript" charset="utf-8" src="model/js/rat-diseases.js"></script>
    <script type="text/javascript" charset="utf-8">Rat.getDiseases('${ratGenes}');</script>

  </c:when>
  <c:otherwise>
    <!-- no rat homologues for this gene -->
    <h3>Diseases (from RatMine)</h3>
    <div><p>No diseases found</p></div>
  </c:otherwise>
</c:choose>

</div>
<!-- /diseaseDisplayer.jsp -->
