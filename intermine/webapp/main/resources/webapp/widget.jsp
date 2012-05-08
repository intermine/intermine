<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<style>
.widget { width:460px; height:auto; max-height:none !important; display:block; float:left; margin:0 10px 20px 10px;
text-align:left; }
.widget header { text-align:left; }
.widget header h3 { margin:0 0 10px 0; }
.widget header p { margin:5px 0; line-height:14px; }
.widget header form div.group { margin-bottom:5px; }
.widget div.content { margin-top:10px; }
.widget div.content table { text-align:left; }
.widget div.content table td,
.widget table thead th { padding:8px; }
.widget div.head div { text-align:left; }
.widget table { border-collapse:collapse; border-spacing:0; width:100%; }
.widget ol { padding:0; }

/* potentially theme specific */
.widget div.popover div.popover-inner { padding:3px; width:280px; overflow:hidden; background:black; background:rgba(0, 0, 0, 0.8);
    border-radius:4px; box-shadow:0 3px 7px rgba(0, 0, 0, 0.3); }
.widget div.popover h3.popover-title { background:#F5F5F5; padding:9px 15px; border-radius:3px 3px 0 0; line-height:1; margin:0; }
.widget div.popover div.popover-content { background:#FFF; padding:10px; }
.widget div.popover a.close { float:right; color:#CCC; font-size:16px; line-height:13px; font-weight:bold; }
.widget div.popover a.close:hover { color:#AAA; }
.widget .btn { background:#ECECEC; border:1px solid #CCC; border-color:rgba(0, 0, 0, 0.1) rgba(0, 0, 0, 0.1) rgba(0, 0, 0, 0.25);
    text-shadow:0 1px 1px rgba(255, 255, 255, 0.75); border-radius:4px; cursor:pointer; }
.widget .btn.disabled { cursor:default; opacity:0.65; }
.widget .btn-mini { padding:2px 6px; font-size:11px; line-height:14px; }

/* theme specific */
.widget { border: 1px solid #D0B5D7; background:#FEFFFF url('../themes/purple/grad_box.png') repeat-x top; }
.widget span.label { background:#5C0075; color:#FFF; border-radius:2px; padding:2px; display:inline-block; }
</style>

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
        <div id="${widgetId}-widget" class="widget"></div>
        <script type="text/javascript">
        (function() {
            var callbacks = {
                matchCb: function(id, type) {
                    window.open(window.service.replace('/service/', '/portal.do?class=' + type + "&externalids=" + id));
                },
                resultsCb: function(pq) {
                    var data = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Results Table</title>" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/css/bootstrap.css\" rel=\"stylesheet\">" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/jquery-1.7.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/underscore-min.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/backbone.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/imjs/src/model.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/imjs/src/service.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/imjs/src/query.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/js/deps.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/js/imtables.js\"></scr"+"ipt>" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/css/tables.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/lib/css/flick/jquery-ui-1.8.19.custom.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/lib/google-code-prettify/prettify.css\" rel=\"stylesheet\">" +
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
        <div id="${widgetId}-widget" class="widget"></div>
        <script type="text/javascript">
        (function() {
            var callbacks = {
                matchCb: function(id, type) {
                    window.open(window.service.replace('/service/', '/portal.do?class=' + type + "&externalids=" + id));
                },
                resultsCb: function(pq) {
                    var data = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Results Table</title>" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/css/bootstrap.css\" rel=\"stylesheet\">" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/jquery-1.7.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/underscore-min.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/backbone.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/imjs/src/model.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/imjs/src/service.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/imjs/src/query.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/js/deps.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/js/imtables.js\"></scr"+"ipt>" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/css/tables.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/lib/css/flick/jquery-ui-1.8.19.custom.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/lib/google-code-prettify/prettify.css\" rel=\"stylesheet\">" +
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
        <div id="${widgetId}-widget" class="widget"></div>
        <script type="text/javascript">
        (function() {
            var callbacks = {
                matchCb: function(id, type) {
                    window.open(window.service.replace('/service/', '/portal.do?class=' + type + "&externalids=" + id));
                },
                resultsCb: function(pq) {
                    var data = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Results Table</title>" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/css/bootstrap.css\" rel=\"stylesheet\">" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/jquery-1.7.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/underscore-min.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/backbone.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/imjs/src/model.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/imjs/src/service.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/lib/imjs/src/query.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/js/deps.js\"></scr"+"ipt>" +
                    "<script src=\"http://alexkalderimis.github.com/im-tables/js/imtables.js\"></scr"+"ipt>" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/css/tables.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/lib/css/flick/jquery-ui-1.8.19.custom.css\" rel=\"stylesheet\">" +
                    "<link href=\"http://alexkalderimis.github.com/im-tables/lib/google-code-prettify/prettify.css\" rel=\"stylesheet\">" +
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