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

                        query.on('list-update:success', function(list) {
                            window.LIST_EVENTS['list-creation:success'](list.title);
                        });
                    });
                },
                resultsCb: function(pq) {
                    var data = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Results Table</title>" +
                    "<link href=\"http://cdn.intermine.org/css/bootstrap/2.0.3-prefixed/css/bootstrap.min.css\" rel=\"stylesheet\">" +
                    "<script src=\"http://cdn.intermine.org/js/jquery/1.7/jquery.min.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/underscore.js/1.3.3/underscore-min.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/backbone.js/0.9.2/backbone-min.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/intermine/imjs/latest/imjs.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/intermine/im-tables/latest/deps.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/intermine/im-tables/latest/imtables.js\"></scr"+"ipt>" +
                    "<link href=\"http://cdn.intermine.org/js/intermine/im-tables/latest/tables.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://cdn.intermine.org/css/jquery-ui/1.8.19/jquery-ui-1.8.19.custom.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://cdn.intermine.org/css/google-code-prettify/latest/prettify.css\" rel=\"stylesheet\">" +
                    "<script type=\"text/javascript\">$(function() { " +
                        "var pq = " + JSON.stringify(pq) + ";" +
                        "var service = new intermine.Service({'root': \"" + service + "\", 'token': \"${token}\"});" +
                        "var view = new intermine.query.results.CompactView(service, pq);" +
                        "view.$el.appendTo(\"#container\"); view.render();" +
                    " });</scr"+"ipt>" +
                    "</head><body><div id=\"container\"></div></body></html>";

                    var w = window.open();
                    w.document.open();
                    w.document.write(data);
                    w.document.close();
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

                        query.on('list-update:success', function(list) {
                            window.LIST_EVENTS['list-creation:success'](list.title);
                        });
                    });
                },
                resultsCb: function(pq) {
                    var data = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Results Table</title>" +
                    "<link href=\"http://cdn.intermine.org/css/bootstrap/2.0.3-prefixed/css/bootstrap.min.css\" rel=\"stylesheet\">" +
                    "<script src=\"http://cdn.intermine.org/js/jquery/1.7/jquery.min.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/underscore.js/1.3.3/underscore-min.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/backbone.js/0.9.2/backbone-min.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/intermine/imjs/latest/imjs.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/intermine/im-tables/latest/deps.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/intermine/im-tables/latest/imtables.js\"></scr"+"ipt>" +
                    "<link href=\"http://cdn.intermine.org/js/intermine/im-tables/latest/tables.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://cdn.intermine.org/css/jquery-ui/1.8.19/jquery-ui-1.8.19.custom.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://cdn.intermine.org/css/google-code-prettify/latest/prettify.css\" rel=\"stylesheet\">" +
                    "<script type=\"text/javascript\">$(function() { " +
                        "var pq = " + JSON.stringify(pq) + ";" +
                        "var service = new intermine.Service({'root': \"" + service + "\", 'token': \"${token}\"});" +
                        "var view = new intermine.query.results.CompactView(service, pq);" +
                        "view.$el.appendTo(\"#container\"); view.render();" +
                    " });</scr"+"ipt>" +
                    "</head><body><div id=\"container\"></div></body></html>";

                    var w = window.open();
                    w.document.open();
                    w.document.write(data);
                    w.document.close();
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

                        query.on('list-update:success', function(list) {
                            window.LIST_EVENTS['list-creation:success'](list.title);
                        });
                    });
                },
                resultsCb: function(pq) {
                    var data = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Results Table</title>" +
                    "<link href=\"http://cdn.intermine.org/css/bootstrap/2.0.3-prefixed/css/bootstrap.min.css\" rel=\"stylesheet\">" +
                    "<script src=\"http://cdn.intermine.org/js/jquery/1.7/jquery.min.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/underscore.js/1.3.3/underscore-min.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/backbone.js/0.9.2/backbone-min.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/intermine/imjs/latest/imjs.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/intermine/im-tables/latest/deps.js\"></scr"+"ipt>" +
                    "<script src=\"http://cdn.intermine.org/js/intermine/im-tables/latest/imtables.js\"></scr"+"ipt>" +
                    "<link href=\"http://cdn.intermine.org/js/intermine/im-tables/latest/tables.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://cdn.intermine.org/css/jquery-ui/1.8.19/jquery-ui-1.8.19.custom.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://cdn.intermine.org/css/google-code-prettify/latest/prettify.css\" rel=\"stylesheet\">" +
                    "<script type=\"text/javascript\">$(function() { " +
                        "var pq = " + JSON.stringify(pq) + ";" +
                        "var service = new intermine.Service({'root': \"" + service + "\", 'token': \"${token}\"});" +
                        "var view = new intermine.query.results.CompactView(service, pq);" +
                        "view.$el.appendTo(\"#container\"); view.render();" +
                    " });</scr"+"ipt>" +
                    "</head><body><div id=\"container\"></div></body></html>";

                    var w = window.open();
                    w.document.open();
                    w.document.write(data);
                    w.document.close();
                }
            };
            window.widgets.table("${widgetId}", "${bagName}", "#${widgetId}-widget", callbacks);
        })();
        </script>
    </c:when>
</c:choose>
<!-- /widget.jsp -->