<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- reportTemplate.jsp -->

<html:xhtml/>

<tiles:importAttribute name="reportObject" ignore="true"/>
<tiles:importAttribute name="interMineIdBag" ignore="true"/>
<tiles:importAttribute name="templateQuery"/>
<tiles:importAttribute name="placement"/>
<tiles:importAttribute name="scope"/>
<tiles:importAttribute name="trail"/>

<c:if test="${!empty param.trail}">
  <c:set var="trail" value="${param.trail}"/>
</c:if>

<c:set var="initValue" value="0"/>
<c:if test="${empty currentUniqueId}">
    <c:set var="currentUniqueId" value="${initValue}" scope="application"/>
</c:if>
<c:set var="tableContainerId" value="_unique_id_${currentUniqueId}" scope="request"/>
<c:set var="currentUniqueId" value="${currentUniqueId + 1}" scope="application"/>


<c:set var="templateName" value="${templateQuery.name}"/>
<c:set var="uid" value="${fn:replace(placement, ' ', '_')}_${templateName}"/>
<c:set var="placementAndField" value="${placement}_${templateName}"/>
<c:set var="useLocalStorage" value="${WEB_PROPERTIES['use.localstorage']=='true'}"/>
<c:set var="expandOnLoad" value="${WEB_PROPERTIES['web.templates.expandonload']=='true'}"/>

<c:choose>
    <c:when test="${reportObject != null}">
        <c:set scope="request" var="tquery" value="${imf:populateTemplateWithObject(templateQuery, reportObject.object)}"/>
    </c:when>
    <c:when test="${interMineIdBag != null}">
        <c:set scope="request" var="tquery" value="${imf:populateTemplateWithBag(templateQuery, interMineIdBag)}"/>
  </c:when>
  <c:otherwise>
    <c:set scope="request" var="tmlType" value="aspect"/>
  </c:otherwise>
</c:choose>


<c:set var="elemId" value="${fn:replace(uid, ':', '_')}"/>

<div class="template" id="${elemId}">
  <im:templateLine scope="${scope}" templateQuery="${templateQuery}" interMineObject="${interMineObject}" bagName="${interMineIdBag.name}" trail="${trail}" templateType="${tmlType}" />
  <p class="description" style="display:none;">${templateQuery.description}</p>

  <%-- JS target for the table --%>
  <div class="collection-table" id="${tableContainerId}"></div>
  <script type="text/javascript">


    (function($) {
        var EXPAND_ON_LOAD = ${expandOnLoad};
        var query = ${tquery.json};
        var disableTemplate = function() {
            $('#${elemId} h3').addClass('no-results').unbind('click');
            $('#${tableContainerId}').remove();
        };
        $(function() {
            $SERVICE.count(query).then(
                function(c) {
                    if (typeof c === 'undefined') {c = 0;}
                    cstr = c.toString()
                    $('#${elemId} h3 span.name').after('<span class="count">(' + cstr + ' rows)</span>');
                    if (c < 1) {
                        disableTemplate();
                    }
                },
                disableTemplate
            );
            $('#${elemId} h3').on('click', function(e) {
                loadTable('#${tableContainerId}', query, 10);
                if(typeof(Storage) !=="undefined"){
                  localStorage.${elemId} = "show";
                }
                $(this).unbind('click').click(function(e) {
                    $('#${tableContainerId}').slideToggle('fast');

                    if(${useLocalStorage} && typeof(Storage) !=="undefined"){
                      if(localStorage.${elemId} == "show"){
                        localStorage.${elemId} = "hide";
                      }else{
                        localStorage.${elemId} = "show";
                      }
                    }
                });
            });
            if(${useLocalStorage} && typeof(Storage)!=="undefined"){
                if (!localStorage.${elemId}) {
                    if (EXPAND_ON_LOAD) {
                        $('#${elemId} h3').click();
                    }
                } else {
                  if(localStorage.${elemId} == "show"){
                     $('#${elemId} h3').click();
                  }
                }
            } else {
                if (EXPAND_ON_LOAD) {
                    $('#${elemId} h3').click();
                }
            }

            function loadTable(elem, query, pageSize) {
                console.debug("Loading table", query);
                var customGalaxy = "${GALAXY_URL}";
                if (customGalaxy) {
                    imtables.configure('Download.Galaxy.Current', customGalaxy);
                }
                imtables.loadDash(
                    elem,
                    {size: pageSize},
                    {service: {root: $SERVICE.root, token: $SERVICE.token}, query: query}
                ).then(
                    withTable,
                    FailureNotification.notify
                );

                function withTable (table) {
                    table.bus.on('list-action:failure', LIST_EVENTS['failure']);
                    table.bus.on('list-action:success', LIST_EVENTS['success']);
                }
            }
        });
    }).call(window, jQuery);
  </script>
</div>

<!-- /reportTemplate.jsp -->
