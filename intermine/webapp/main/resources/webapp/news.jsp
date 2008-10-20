<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<html:xhtml/>

<!-- news.jsp -->
<script type="text/javascript">
jQuery(document).ready(function(){
              AjaxServices.getNewsRead('${WEB_PROPERTIES['project.rss']}',function(html){
                jQuery('#newsbox').html(html);
              });
           });
</script>

   <div class="gradientbox" style="width:50%">
      <h1 style="display:inline">News</h2>
      <div id="newsbox"><div align="center"><br/><br/><br/><img src="images/wait18.gif" title="Getting news..."/></div></div>
      <a href="${WEB_PROPERTIES['project.sitePrefix']}/news.shtml">more...</a>
    </div>
 <!-- /news.jsp -->