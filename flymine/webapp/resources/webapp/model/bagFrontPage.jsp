<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<p>"Bags" are holders for lists of data (e.g. a list of gene identifiers) and allow you to analyse (expression data,...), identify common characteristics (GO terms,...) and to run templates directly on these lists.</p>

<p>Have a look at some examples:</p>
<html:link action="/bagDetails.do?bagName=example+bag&scope=global">Example Bag</html:link>

<p>...or Create your own:</p>
<html:link action="/mymine.do?page=bags"><img src="images/go_to_bag_page.png" align="center" width="317" height="80" alt="Go To Bag Page" border="0"></html:link>
