<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- tipWrapper.jsp -->
  <script type="text/javascript" src="js/niftycube.js"></script>
  <link rel="stylesheet" type="text/css" href="css/tips.css"/>
    <script type="text/javascript">
      window.onload=function(){
         Nifty("div#tipbox","big");
      }
    </script>
    <div id="tipbox" >
       <tiles:insert name="tip.tile" />
    </div>

<!-- /tip.jsp -->
