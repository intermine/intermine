<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- orthologueLinkDisplayer.jsp -->

<table class="lookupReport" cellspacing="5" cellpadding="0">
  <c:forEach var="mineEntry" items="${mines}" varStatus="status">

    <c:set var="mine" value="${mineEntry.key}"/>
    <c:set var="orthologuesToDatasets" value="${mineEntry.value}"/>

    <c:set var="imageName" value="${mine.logo}"/>
    <c:set var="mineName" value="${mine.name}"/>

    <tr>
    <td align="right">
        <c:if test="${!empty imageName}">
         <a href="#orthologue_link_${status.count}" title="${mineName}" class="boxy" >
               <html:img src="model/images/${imageName}" title="${mineName}"/>
         </a>
        </c:if>
    </td>
    <td>
        <a href="#orthologue_link_${status.count}" title="${mineName}" class="boxy" >
            ${mineName}&nbsp;<img src="images/ext_link.png" title="${mineName}"/>
        </a>

    <%-- orthologue link popup --%>
    <div id="orthologue_link_${status.count}" style="display:none">
        You are being forwarded to ${mineName}
        <br/><br/>
        <table>
        <tr>
            <td valign="top"><b>Orthologues</b></td>
            <td valign="top"><div id="orthologue_organismName_${status.count}">${mine.defaultOrganismName}</div></td>
            <td valign="top">[<a href="#orthologue_link_config_${status.count}" title="Orthologues" class="boxy" >edit</a>]</td>
        </tr>
        <tr>
           <td valign="top"><b>Mapping</b></td>
           <td valign="top"><div id="orthologue_mapping_${status.count}">${mine.defaultMapping}</div></td>
           <td valign="top">[<a href="#orthologue_link_config_${status.count}" title="Orthologues" class="boxy" >edit</a>]</td>
        </tr>
        </table>

        <br/><br/>

        <form action="${mine.url}/portal.do" method="post" name="orthologueLinkForm${statusCount}" target="_blank">
        <input type="hidden" name="externalids" value="${identifierList}"/>
        <input type="hidden" name="class" value="${bag.type}"/>
        <input type="hidden" name="orthologue" value="${mine.defaultOrganismName}"/>
        
        <input type="submit" name="submit" value="GO"/>
        </form>
    </div>

    <%-- orthologue link config popup --%>
    <div id="orthologue_link_config_${status.count}" style="display:none">

        Select orthologues to view:
        <br/><br/>
        <table>
        <c:forEach var="entry" items="${orthologuesToDatasets}" varStatus="entryStatus">

        <c:set var="orthologue" value="${entry.key}"/>
        <c:set var="datasets" value="${entry.value}"/>

        <tr>
            <td><div id="orthologue_link_config_option_${entryStatus.count}" onclick="javascript:updateOrthologueLinks('${status.count}', '${entryStatus.count}', '${orthologue}');" <c:if test="${mine.defaultOrganismName == orthologue}">class="selected"</c:if>>${orthologue}</div></td>
            <%--
            this clutters up the display, disable for now.
            <td>
                <c:forEach var="dataset" items="${datasets}" varStatus="datasetStatus">
                    <c:if test="${datasetStatus.count != 1}">,</c:if>&nbsp;${dataset}
                </c:forEach>
            </td>
             --%>
        </tr>

        </c:forEach>
        </table>
        
            <br/><br/>
        
            <a href="#" class="close">[done]</a>
        
            <c:set var="orthologueCount" value="${fn:length(orthologuesToDatasets)}"/>
        <br/><br/>

    </div>

    </td>
    </tr>
</c:forEach>
</table>

<script type="text/javascript" charset="utf-8">
    jQuery(document).ready(function(){
        jQuery(".boxy").boxy();
    });
    function updateOrthologueLinks(statusCount, orthoStatusCount, orthologue) {
        document.getElementById('orthologue_organismName_' + statusCount).innerHTML=orthologue;
        document.orthologueLinkForm.orthologue.value=orthologue;
        for (i=1;i<=${orthologueCount};i++) {
            if (i != orthoStatusCount) {
                document.getElementById('orthologue_link_config_option_' + i).className='';
            } else {
                document.getElementById('orthologue_link_config_option_' + i).className='selected';
            }
        }
    }
</script>

<!-- /orthologueLinkDisplayer.jsp -->
