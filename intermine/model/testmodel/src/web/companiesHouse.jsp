<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- companiesHouse.jsp -->
<%-- Display an icon linked to companys house --%>
<html:link href="http://ws3info.companieshouse.gov.uk/info/do_search.cgi?cname=${object.name}">
  <html:img src="model/companiesHouse.png"/>
</html:link>
<!-- /companiesHouse.jsp -->
