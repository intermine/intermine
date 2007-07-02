<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- tip.jsp -->
<script type="text/javascript" src="style/niftycube.js"></script>
<script type="text/javascript">
window.onload=function(){
Nifty("div#tipbox","big");
}
</script>
<div class="body" align="left">
	<div id="tipbox">
	<h3>Did you know?</h3>
		<tiles:insert name="tip.tile" />
	</div>
</div>

<!-- /tip.jsp -->
