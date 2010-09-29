<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!-- orthologueLinkDisplayer.jsp -->

<form id="orthologueLinkForm" name="orthologueLinkForm" target="_blank">

    <input type="hidden" id="originalExternalids" name="originalExternalids" value="${identifierList}"/>
    <input type="hidden" id="externalids" name="externalids" value="${identifierList}"/>
    <input type="hidden" id="class" name="class" value="${bag.type}"/>

<table class="lookupReport" cellspacing="5" cellpadding="0">


  <c:forEach var="mineEntry" items="${mines}" varStatus="status">

    <c:set var="mine" value="${mineEntry.key}"/>
    <c:set var="orthologuesToDatasets" value="${mineEntry.value}"/>
    <c:set var="imageName" value="${mine.logo}"/>
    <c:set var="mineName" value="${mine.name}"/>

    <tr>
    <td align="right">
        <c:if test="${!empty imageName}">
         <a href="#orthologue_link_${status.count}" onclick="checkOrthologueMapping('${status.count}', '${mineName}', '${WEB_PROPERTIES['project.title']}');" title="${mineName}" class="boxy" >
               <html:img src="model/images/${imageName}" title="${mineName}"/>
         </a>
        </c:if>
    </td>
    <td>
        <a href="#orthologue_link_${status.count}" onclick="checkOrthologueMapping('${status.count}', '${mineName}', '${WEB_PROPERTIES['project.title']}');" title="${mineName}" class="boxy" >
            ${mineName}&nbsp;<img src="images/ext_link.png" title="${mineName}"/>
        </a>

    <%-- orthologue link popup --%>

    <div id="orthologue_link_${status.count}" style="display:none;">

        You are exporting your list to ${mineName}
        <br/><br/>
        <table>
        <tr>
            <td valign="top"><b>Orthologues</b></td>
            <td valign="top">
            <span id="orthologueSelect${status.count}" style="float:left;">
            <select name="orthologueDatasets${status.count}" id="orthologueDatasets${status.count}" onchange="checkOrthologueMapping('${status.count}', '${mineName}', '${WEB_PROPERTIES['project.title']}');">
            <c:forEach var="entry" items="${orthologuesToDatasets}" varStatus="entryStatus">
                <c:set var="orthologue" value="${entry.key}"/>
                <c:set var="datasets" value="${entry.value}"/>
                <c:set var="localMapping" value="${datasets.localDataSetsString}"/>
                <c:set var="remoteMapping" value="${datasets.remoteDataSetsString}"/>
                <option value="${localMapping}|${remoteMapping}" <c:if test="${mine.defaultOrganismName == orthologue}">selected</c:if>>${orthologue}</option>
            </c:forEach>
            </select>
            </span>
            <%-- if there is only one option, just display organism instead of a dropdown select box --%>
            <span id="orthologueSelectDisplay${status.count}" style="display:none;float:left;">
            ${mine.defaultOrganismName}
            </span>
            </td>
        </tr>
        <tr>
           <td valign="top"><b>Mapping</b></td>
           <td valign="top">

                <c:set var="remoteChecked" value="false"/>
                <c:set var="localChecked" value="false"/>

           <c:if test="${mine.defaultMapping == 'remote'}"><c:set var="remoteChecked" value="true"/></c:if>
           <c:if test="${mine.defaultMapping == 'local'}"><c:set var="localChecked" value="true"/></c:if>

                <span id="orthologueMappingRemoteRadio${status.count}" style="float:left;"><input type="radio" id="orthologueMapping${status.count}Remote" name="orthologueMapping${status.count}" checked="${remoteChecked}" value="remote"></span><span id="orthologueMappingRemoteLabel${status.count}" style="float:left;">${mineName}</span>
                <span id="orthologueMappingLocalRadio${status.count}" style="float:left;"><input type="radio" id="orthologueMapping${status.count}Local" name="orthologueMapping${status.count}" checked="${localChecked}" value="local"></span><span id="orthologueMappingLocalLabel${status.count}" style="float:left;">${WEB_PROPERTIES['project.title']}</span>
           </td>
        </tr>
        </table>
        <br/><br/>

        <input type="button" name="submitButton${status.count}" value="GO" onClick="javascript:submitOrthologueLinkForm('${bag.type}', '${bag.name}', '${status.count}');" />
        <input type="hidden" id="orthologue${status.count}" name="orthologue${status.count}" value="${mine.defaultOrganismName}"/>
        <input type="hidden" id="formAction${status.count}" name="formAction${status.count}" value="${mine.url}/portal.do"/>

    </div>
    </td>
    </tr>
</c:forEach>
</table>
 </form>


<script type="text/javascript" charset="utf-8">
<!--//<![CDATA[
    jQuery(document).ready(function(){
        jQuery(".boxy").boxy();
    });

    function newBoxy(index) {
        new Boxy(jQuery('#orthologue_link_' + index), {title: "Constraint for ", unloadOnHide: true});
    }

//]]>-->
</script>

<!-- /orthologueLinkDisplayer.jsp -->
