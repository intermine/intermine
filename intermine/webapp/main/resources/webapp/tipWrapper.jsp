<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- tipWrapper.jsp -->
<html style="background-color: #fff">
  <head>
    <title>FlyMine Tips</title>
    <link rel="stylesheet" type="text/css" href="css/webapp.css"/>
    <link rel="stylesheet" type="text/css" href="css/tips.css"/>
    <link rel="stylesheet" type="text/css" href="model/model.css"/>
    <script type="text/javascript" src="js/niftycube.js"></script>
  </head>
  <body style="background-color: #fff">
    <script type="text/javascript">
      window.onload=function(){
         Nifty("div#tipbox","big");
      }
    </script>
    <div class="body">
      <div id="tipbox" >
        <h3>Did you know?</h3>
        <tiles:insert name="tip.tile" />
      </div>
    </div>
  </body>
</html>

<!-- /tip.jsp -->
