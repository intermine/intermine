<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mm"%>

<!-- submissionFileDownloadDisplayer.jsp -->

<div id="file-download-displayer" style="padding-top:20px;">
    <h3>Data Files Download</h3>

    <%-- FILES --%>
    <c:if test="${!empty files}">
         <div class="filelink" style="padding:5px;">
            <mm:dataFiles files="${files}" dccId="${dCCid}"/>
         </div>
    </c:if>

    <%-- TARBALL --%>
    <div style="padding:2px;">
      <b>
        <mm:getTarball dccId="${dCCid}"/>
      </b>
    </div>

</div>

<!-- /submissionFileDownloadDisplayer.jsp -->