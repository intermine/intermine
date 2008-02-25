<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>


<!-- BDGPLinkOut.jsp -->
<a href="http://www.fruitfly.org/cgi-bin/ex/bquery.pl?qtype=report&find=${object.gene.secondaryIdentifier}&searchfield=CG" target="_new" class="ext_link"><img src="model/images/BDGP_logo.gif"/> ${object.gene.secondaryIdentifier}</a>
<!-- /BDGPLinkOut.jsp -->