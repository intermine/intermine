<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- widget.jsp -->
<tiles:importAttribute name="widget" ignore="false" />
<tiles:importAttribute name="bag" ignore="false" />

<html:xhtml/>
<c:set var="split" value="${fn:split(widget.class,'.')}"/>
<c:set var="type" value="${split[fn:length(split)-1]}"/>
<c:set var="bagName" value="${bag.name}"/>
<c:set var="widgetId" value="${widget.id}"/>

<c:choose>
    <c:when test="${type == 'GraphWidgetConfig'}" >
        <div id="${widgetId}-widget" class="bootstrap widget"></div>
        <script type="text/javascript">
        (function() {
            var callbacks = {
                matchCb: function(id, type) {
                    window.open(window.service.replace('/service/', '/portal.do?class=' + type + "&externalids=" + id));
                },
                listCb: function(pq) {
                    var service = new intermine.Service({'root': window.service, 'token': "${token}"});
                    service.query(pq, function(query) {
                        var dialogue = new intermine.query.actions.ListCreator(query);
                        dialogue.render().$el.appendTo('#${widgetId}-widget');
                        dialogue.openDialogue();

                        query.on('list-creation:success', window.LIST_EVENTS['list-creation:success']);
                        query.on('list-creation:failure', window.LIST_EVENTS['list-creation:failure']);
                    });
                },
                resultsCb: function(pq) {
					(new intermine.Service({'root': service})).query(pq, function(query) {						
						window.open(service.replace('/service/', "run.do") + "?query=" + query.toXML());
						window.focus();
					});
                }
            };
            window.widgets.chart("${widgetId}", "${bagName}", "#${widgetId}-widget", callbacks);
        })();
        </script>
    </c:when>
    <c:when test="${type == 'EnrichmentWidgetConfig'}" >
        <div id="${widgetId}-widget" class="bootstrap widget"></div>
        <script type="text/javascript">
        (function() {
            var callbacks = {
                matchCb: function(id, type) {
                    window.open(window.service.replace('/service/', '/portal.do?class=' + type + "&externalids=" + id));
                },
                listCb: function(pq) {
                    var service = new intermine.Service({'root': window.service, 'token': "${token}"});
                    service.query(pq, function(query) {
                        var dialogue = new intermine.query.actions.ListCreator(query);
                        dialogue.render().$el.appendTo('#${widgetId}-widget');
                        dialogue.openDialogue();

                        query.on('list-creation:success', window.LIST_EVENTS['list-creation:success']);
                        query.on('list-creation:failure', window.LIST_EVENTS['list-creation:failure']);
                    });
                },
                resultsCb: function(pq) {
					(new intermine.Service({'root': service})).query(pq, function(query) {						
						window.open(service.replace('/service/', "run.do") + "?query=" + query.toXML());
						window.focus();
					});
                }
            };
            window.widgets.enrichment("${widgetId}", "${bagName}", "#${widgetId}-widget", callbacks);
        })();
        </script>
    </c:when>
    <c:when test="${type == 'TableWidgetConfig'}" >
        <div id="${widgetId}-widget" class="bootstrap widget"></div>
        <script type="text/javascript">
        (function() {
            var callbacks = {
                matchCb: function(id, type) {
                    window.open(window.service.replace('/service/', '/portal.do?class=' + type + "&externalids=" + id));
                },
                listCb: function(pq) {
                    var service = new intermine.Service({'root': window.service, 'token': "${token}"});
                    service.query(pq, function(query) {
                        var dialogue = new intermine.query.actions.ListCreator(query);
                        dialogue.render().$el.appendTo('#${widgetId}-widget');
                        dialogue.openDialogue();

                        query.on('list-creation:success', window.LIST_EVENTS['list-creation:success']);
                        query.on('list-creation:failure', window.LIST_EVENTS['list-creation:failure']);
                    });
                },
                resultsCb: function(pq) {
					(new intermine.Service({'root': service})).query(pq, function(query) {						
						window.open(service.replace('/service/', "run.do") + "?query=" + query.toXML());
						window.focus();
					});
                }
            };
            window.widgets.table("${widgetId}", "${bagName}", "#${widgetId}-widget", callbacks);
        })();
        </script>
    </c:when>
</c:choose>
<!-- /widget.jsp -->
