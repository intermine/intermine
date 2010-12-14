<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<html:xhtml/>

<!-- news.jsp -->
<script type="text/javascript">
    // feed URL
    var feedURL = "${WEB_PROPERTIES['project.rss']}";
    // limit number of entries displayed
    var maxEntries = 4;
    // where are we appending entries? (jQuery syntax)
    var target = '#newsbox';

    var months = new Array(12); months[0]="Jan"; months[1]="Feb"; months[2]="Mar"; months[3]="Apr"; months[4]="May"; months[5]="Jun";
    months[6]="Jul"; months[7]="Aug"; months[8]="Sep"; months[9]="Oct"; months[10]="Nov"; months[11]="Dec";

    $(document).ready(function() {
        // DWR fetch, see AjaxServices.java
        AjaxServices.getNewsPreview(feedURL, function(data) {
            if (data) {
                // show us
                $('#spinner').toggle();
                $(target).append('<ol id="news">');

                // declare
                var feedTitle, feedDescription, feedDate, feedLink, element, feed;

                // convert to XML, jQuery manky...
                try {
                    // Internet Explorer
                    feed = new ActiveXObject("Microsoft.XMLDOM");
                    feed.async="false";
                    feed.loadXML(data);
                } catch(e) {
                    try {
                        // ...the good browsers
                        feed = new DOMParser().parseFromString(data, "text/xml");
                    } catch(e) {
                        // ... BFF
                        alert(e.message);
                        return;
                    }
                }

                var items = feed.getElementsByTagName("item"); // ATOM!!!
                for (var i = 0; i < items.length; ++i) {
                    // early bath
                    if (i > maxEntries) return;

                    feedTitle = trimmer(items[i].getElementsByTagName("title")[0].firstChild.nodeValue, 80);
                    feedDescription = trimmer(items[i].getElementsByTagName("description")[0].firstChild.nodeValue, 140);
                    feedDate = new Date(items[i].getElementsByTagName("pubDate")[0].firstChild.nodeValue);
                    feedLink = items[i].getElementsByTagName("link")[0].firstChild.nodeValue

                       // build list element
                       element = '<li>'
                                   + '<a target="new" href="' + feedLink + '">' + feedTitle + '</a> &ndash; '
                                       + '<em>' + feedDate.getDate() + ' ' + months[feedDate.getMonth()] + ' ' + feedDate.getFullYear() + '</em>'
                                       + '<p>' + feedDescription + '</p>'
                               + '</li>';
                       // append, done
                       $(target).append(element);
                       i++;
                }
                $(target).append('</ol>');
            }
        });
    });

          // trim text to a specified length
    function trimmer(grass, length) {
        if (!grass) return;
        grass = stripHTML(grass);
        if (grass.length > length) return grass.substring(0, length) + ' [...]';
        return grass;
    }

    // strip HTML
          function stripHTML(html) {
             var tmp = document.createElement("DIV"); tmp.innerHTML = html; return tmp.textContent || tmp.innerText;
          }
</script>
   <div class="plainbox" fixedWidth="300px">
      <h1 style="display:inline">News</h1>
      <div id="newsbox">
          <div id="spinner" align="center"><br/><br/><br/><img src="images/wait18.gif" title="Getting news..."/></div>
      </div>
      <a href="${WEB_PROPERTIES['project.news']}">more...</a>
   </div>
 <!-- /news.jsp -->
