<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- begin.jsp -->
<html:xhtml/>

<div id="content-wrap">
        <div id="boxes">
                <div id="search-bochs">
                        <img class="title" src="themes/purple/homepage/search-ico-right.png" title="search"/>
                        <div class="inner">
                                <h3><c:out value="${WEB_PROPERTIES['begin.searchBox.title']}" /></h3>
                                <span class="ugly-hack">&nbsp;</span>
                                <p><c:out value="${WEB_PROPERTIES['begin.searchBox.description']}" escapeXml="false" /></p>

                                <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
                                        <div class="input"><input id="actionsInput" name="searchTerm" class="input" type="text" value="${WEB_PROPERTIES['begin.searchBox.example']}"></div>
                                        <div class="bottom">
                                                <center>
                                                        <input id="mainSearchButton" name="searchSubmit" class="button dark" type="submit" value="search"/>
                                                </center>
                                        </div>
                                </form>
                                <div style="clear:both;"></div>
                        </div>
                </div>
                <div id="lists-bochs">
                        <img class="title" src="images/icons/lists-64.png" title="lists"/>
                        <div class="inner">
                                <h3><c:out value="${WEB_PROPERTIES['begin.listBox.title']}" /></h3>
                                <p><c:out value="${WEB_PROPERTIES['begin.listBox.description']}" escapeXml="false" /></p>

                                <form name="buildBagForm" method="post" action="<c:url value="/buildBag.do" />">
                                        <select name="type">
                                            <c:forEach var="bag" items="${preferredBags}">
                                                <option value="<c:out value="${bag}" />"><c:out value="${imf:formatPathStr(bag, INTERMINE_API, WEBCONFIG)}" /></option>
                                            </c:forEach>
                                        </select>

                        <c:if test="${!empty WEB_PROPERTIES['begin.listUpload.values']}">
                        <tr>
                                <td align="right" class="label">
                                             <label>
                                                 <fmt:message key="bagBuild.extraConstraint">
                                                             <fmt:param value="${extraBagQueryClass}"/>
                                                 </fmt:message>
                                             </label>
                                     </td>
                                     <td>
                                         <select name="extraFieldValue">
                                            <c:forEach var="value" items="${WEB_PROPERTIES['begin.listUpload.values']}">
                                                <option value="<c:out value="${value}" />"><c:out value="${value}" /></option>
                                            </c:forEach>
                                        </select>
                                 </td>
                        </tr>
                </c:if>

                                        <div class="textarea">
                                            <c:choose>
                                                <c:when test="${fn:startsWith(WEB_PROPERTIES['bag.example.identifiers'], 'e.g') == true}">
                                                    <textarea id="listInput" name="text"><c:out value="${WEB_PROPERTIES['bag.example.identifiers']}" /></textarea>
                                                </c:when>
                                                <c:otherwise>
                                                    <textarea id="listInput" name="text">e.g. <c:out value="${WEB_PROPERTIES['bag.example.identifiers']}" /></textarea>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="bottom">
                                                <center>
                                                        <a class="advanced" href="bag.do?subtab=upload">advanced</a>
                                                        <br />
                                                        <input class="button light" type="submit" value="analyse"/>
                                                </center>
                                        </div>
                                </form>
                        </div>
                </div>
                <div id="welcome-bochs">
                        <div class="inner">
                            <c:choose>
                                <c:when test="${!isNewUser && !empty (WEB_PROPERTIES['begin.thirdBox.visitedTitle'])}">
                                    <h3><c:out value="${WEB_PROPERTIES['begin.thirdBox.visitedTitle']}" /></h3>
                                </c:when>
                                <c:otherwise>
                                    <h3><c:out value="${WEB_PROPERTIES['begin.thirdBox.title']}" /></h3>
                                </c:otherwise>
                            </c:choose>
                                <br />
                                <c:choose>
                                    <c:when test="${!isNewUser && !empty (WEB_PROPERTIES['begin.thirdBox.visitedDescription'])}">
                                        <p><c:out value="${WEB_PROPERTIES['begin.thirdBox.visitedDescription']}" escapeXml="false" /></p>
                                    </c:when>
                                    <c:otherwise>
                                        <p><c:out value="${WEB_PROPERTIES['begin.thirdBox.description']}" escapeXml="false" /></p>
                                    </c:otherwise>
                                </c:choose>
                                <c:if test="${!empty WEB_PROPERTIES['begin.thirdBox.linkTitle']}">
                                    <div class="bottom">
                                            <center>
                                                <c:choose>
                                                    <c:when test="${!isNewUser && !empty (WEB_PROPERTIES['begin.thirdBox.visitedLink'])}">
                                                        <a class="button gray" href="<c:out value="${WEB_PROPERTIES['begin.thirdBox.visitedLink']}" />"
                                                        onclick="javascript:window.open('<c:out value="${WEB_PROPERTIES['begin.thirdBox.visitedLink']}" />','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">
                                                    </c:when>
                                                    <c:otherwise>
                                                        <a class="button gray" href="<c:out value="${WEB_PROPERTIES['begin.thirdBox.link']}" />"
                                                        onclick="javascript:window.open('<c:out value="${WEB_PROPERTIES['begin.thirdBox.link']}" />','_help','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">
                                                    </c:otherwise>
                                                </c:choose>
                                                        <div>
                                                            <span>
                                                                <c:choose>
                                                                    <c:when test="${!isNewUser && !empty (WEB_PROPERTIES['begin.thirdBox.visitedLinkTitle'])}">
                                                                        <c:out value="${WEB_PROPERTIES['begin.thirdBox.visitedLinkTitle']}" />
                                                                    </c:when>
                                                                    <c:otherwise>
                                                                        <c:out value="${WEB_PROPERTIES['begin.thirdBox.linkTitle']}" />
                                                                    </c:otherwise>
                                                                </c:choose>
                                                            </span>
                                                        </div>
                                                    </a>
                                            </center>
                                    </div>
                                </c:if>
                        </div>
                </div>
        </div>


        <div style="clear:both"></div>
	<!-- 
	<div class="topBar messages"> <br/>MESSAGE TO CURATORS: To put your template here tag it with im:public, im:aspect:ACPECTNAME, and im:frontpage.  
	<br/>ASPECTNAME must be correctly capitalized and may contain spaces.  Proper capitalization may be found in Templates -&gt; filter drop down menu.  In this case, the first letter of each word is capitalized.</br>-JD</div>
	
	-->
        <div id="bottom-wrap">
            <c:if test="${!empty tabs}">
                <div id="templates">
                        <table id="menu" border="0" cellspacing="0">
                                <tr>
                                    <!-- templates tabs -->
                                    <c:forEach var="item" items="${tabs}">
                                        <td><div class="container"><span id="tab${item.key}">
                                            <c:forEach var="row" items="${item.value}">
                                                <c:choose>
                                                    <c:when test="${row.key == 'name'}">
                                                        <c:out value="${row.value}" />
                                                    </c:when>
                                                </c:choose>
                                            </c:forEach>
                                        </span></div></td>
                                    </c:forEach>
                                </tr>
                        </table>

                        <div id="tab-content">
                                <div id="try"></div>

                                <!-- templates content -->
                                <c:forEach var="item" items="${tabs}">
                                    <div id="content${item.key}" class="content">
                                        <c:forEach var="row" items="${item.value}">
                                            <c:choose>
                                                <c:when test="${row.key == 'identifier'}">
                                                    <c:set var="aspectTitle" value="${row.value}"/>
                                                </c:when>
                                                <c:when test="${row.key == 'name'}">
                                                    <p>Query for <c:out value="${fn:toLowerCase(row.value)}" />:</p>
                                                </c:when>
                                                <c:when test="${row.key == 'templates'}">
                                                    <ul>
                                                        <c:forEach var="template" items="${row.value}">
                                                            <li><a href="template.do?name=${template.name}&scope=global"><c:out value="${fn:replace(template.title,'-->','&nbsp;<img src=\"images/icons/green-arrow-16.png\" style=\"vertical-align:bottom\">&nbsp;')}" escapeXml="false" /></a></li>
                                                        </c:forEach>
                                                    </ul>
                                                    <p class="more"><a href="templates.do?filter=${aspectTitle}">More queries</a></p>
                                                </c:when>
                                            </c:choose>
                                        </c:forEach>
                                    </div>
                                </c:forEach>
                        </div>
                </div>
            </c:if>

    <c:if test="${fn:length(frontpageBags) > 0}">
        <div id="lists">
            <h4>Lists</h4>
            <p><c:out value="${WEB_PROPERTIES['begin.listsBox.description']}" /></p>
            <ul>
                <c:forEach var="bag" items="${frontpageBags}">
                <li>
                    <h5><a href="bagDetails.do?scope=all&bagName=<c:out value="${fn:replace(bag.value.title, ' ', '+')}"/>">${bag.value.title}</a></h5>
                    <span>(${bag.value.size}&nbsp;<b>${bag.value.type}<c:if test="${bag.value.size > 1}">s</c:if></b>)</span>
                    <c:if test="${!empty(bag.value.description)}">
                        <p>${bag.value.description}</p>
                    </c:if>
                </li>
                </c:forEach>
            </ul>

            <p class="more">
                <a href="bag.do?subtab=view">More lists</a>
            </p>
        </div>
    </c:if>

                <div id="low">
                        <div id="rss" style="display:none;">
                                <h4>News<span>&nbsp;&amp;&nbsp;</span>Updates</h4>
                                <table id="articles"></table>
                                <c:if test="${!empty WEB_PROPERTIES['links.blog']}">
                                    <p class="more"><a target="new" href="${WEB_PROPERTIES['links.blog']}">More news</a></p>
                                </c:if>
                        </div>

                        <div id="api">
                                <h4>Perl, Python, Ruby and <span>&nbsp;&amp;&nbsp;</span> Java API</h4>
                                <img src="images/begin/java-perl-python-ruby-2.png" alt="perl java python ruby" />
                                <p>
                                        Access our <c:out value="${WEB_PROPERTIES['project.title']}"/> data via
                                        our Application Programming Interface (API) too!
                                        We provide client libraries in the following languages:
                                </p>
                                <ul id="api-langs">
                                        <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />api.do?subtab=perl">Perl</a>
                                        <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />api.do?subtab=python">Python</a>
                                        <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />api.do?subtab=ruby">Ruby</a>
                                        <li><a href="<c:out value="${WEB_PROPERTIES['path']}" />api.do?subtab=java">Java</a>
                                </ul>
                        </div>

                        <div style="clear:both;"></div>
                </div>
        </div>
</div>

<script type="text/javascript">
        jQuery(document).ready(function() {
                jQuery("#tab-content .content").each(function() {
                        jQuery(this).hide();
                });

                jQuery("table#menu td:first").addClass("active").find("div").append('<span class="right"></span><span class="left"></span>').show();
                jQuery("div.content:first").show();

                jQuery("table#menu td").click(function() {
                        jQuery("table#menu td.active").find("div").find('.left').remove();
                        jQuery("table#menu td.active").find("div").find('.right').remove();
                        jQuery("table#menu td").removeClass("active");

                        jQuery(this).addClass("active").find("div").append('<span class="right"></span><span class="left"></span>');
                        jQuery("#tab-content .content").hide();

                        if (jQuery(this).is('span')) {
                                // span
                                var activeTab = jQuery(this).attr("id").substring(3);
                        } else {
                                // td, div (IE)
                                var activeTab = jQuery(this).find("span").attr("id").substring(3);
                        }
                        jQuery('#content' + activeTab).fadeIn();

                        return false;
                });
        });


        // feed URL
        var feedURL = "${WEB_PROPERTIES['project.rss']}";
        // limit number of entries displayed
        var maxEntries = 3;
        // where are we appending entries? (jQuery syntax)
        var target = 'table#articles';

        var months = new Array(12); months[0]="Jan"; months[1]="Feb"; months[2]="Mar"; months[3]="Apr"; months[4]="May"; months[5]="Jun";
        months[6]="Jul"; months[7]="Aug"; months[8]="Sep"; months[9]="Oct"; months[10]="Nov"; months[11]="Dec";

        $(document).ready(function () {
                // DWR fetch, see AjaxServices.java
                AjaxServices.getNewsPreview(feedURL, function (data) {
                        if (data) {
                                // show us
                                $('#rss').slideToggle('slow');

                                // declare
                                var feedTitle, feedDescription, feedDate, feedLink, row, feed;

                                // convert to XML, jQuery manky...
                                try {
                                        // Internet Explorer
                                        feed = new ActiveXObject("Microsoft.XMLDOM");
                                        feed.async = "false";
                                        feed.loadXML(data);
                                } catch (e) {
                                        try {
                                                // ...the good browsers
                                                feed = new DOMParser().parseFromString(data, "text/xml");
                                        } catch (e) {
                                                // ... BFF
                                                alert(e.message);
                                                return;
                                        }
                                }

                                var items = feed.getElementsByTagName("item"); // ATOM!!!
                                for (var i = 0; i < items.length; i++) {
                                        // early bath
                                        if (i == maxEntries) return;

                                        feedTitle = trimmer(items[i].getElementsByTagName("title")[0].firstChild.nodeValue, 70);
                                        feedDescription = (items[i].getElementsByTagName("description").length > 0) ?
                                                trimmer(items[i].getElementsByTagName("description")[0].firstChild.nodeValue, 70) : '';
                                        // we have a feed date
                                        if (items[i].getElementsByTagName("pubDate")[0]) {
                                                feedDate = new Date(items[i].getElementsByTagName("pubDate")[0].firstChild.nodeValue);
                                                feedLink = items[i].getElementsByTagName("link")[0].firstChild.nodeValue

                                                // build table row
                                                row = '<tr>' + '<td class="date">' + '<a target="new" href="' + feedLink + '">' + feedDate.getDate()
                                                + '<br /><span>' + months[feedDate.getMonth()] + '</span></a></td>'
                                                + '<td><a target="new" href="' + feedLink + '">' + feedTitle + '</a><br/>' + feedDescription + '</td>'
                                                + '</tr>';
                                        } else {
                                                feedLink = items[i].getElementsByTagName("link")[0].firstChild.nodeValue

                                                // build table row
                                                row = '<tr>'
                                                + '<td><a target="new" href="' + feedLink + '">' + feedTitle + '</a><br/>' + feedDescription + '</td>'
                                                + '</tr>';
                                        }

                                        // append, done
                                        $(target).append(row);
                                }
                        }
                });
        });

                // trim text to a specified length
        function trimmer(grass, length) {
                if (!grass) return;
                grass = stripHTML(grass);
                if (grass.length > length) return grass.substring(0, length) + '...';
                return grass;
        }

        // strip HTML
        function stripHTML(html) {
                var tmp = document.createElement("DIV"); tmp.innerHTML = html; return tmp.textContent || tmp.innerText;
        }

        var placeholder = '<c:out value="${WEB_PROPERTIES['begin.searchBox.example']}" />';
        var placeholderTextarea = 'e.g. <c:out value="${WEB_PROPERTIES['bag.example.identifiers']}" />';
        var inputToggleClass = 'eg';

        // e.g. values only available when JavaScript is on
        jQuery('input#actionsInput').toggleClass(inputToggleClass);
        jQuery('textarea#listInput').toggleClass(inputToggleClass);

        // register input elements with blur & focus
        jQuery('input#actionsInput').blur(function() {
                if (jQuery(this).val() == '') {
                        jQuery(this).toggleClass(inputToggleClass);
                        jQuery(this).val(placeholder);
                }
        });
        jQuery('textarea#listInput').blur(function() {
                if (jQuery(this).val() == '') {
                        jQuery(this).toggleClass(inputToggleClass);
                        jQuery(this).val(placeholderTextarea);
                }
        });
        jQuery('input#actionsInput').focus(function() {
                if (jQuery(this).hasClass(inputToggleClass)) {
                        jQuery(this).toggleClass(inputToggleClass);
                        jQuery(this).val('');
                }
        });
        jQuery('textarea#listInput').focus(function() {
                if (jQuery(this).hasClass(inputToggleClass)) {
                        jQuery(this).toggleClass(inputToggleClass);
                        jQuery(this).val('');
                }
        });
</script>

<!-- /begin.jsp -->
