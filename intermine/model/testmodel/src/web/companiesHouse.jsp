<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- CompaniesHouseIcon.jsp -->
<%-- Display an icon linked to companys house --%>
<html:link href="http://ws3info.companieshouse.gov.uk/info/do_search.cgi?cname=${object.name}">
  <html:img src="model/companiesHouse.png"/>
</html:link>
<!-- /CompaniesHouseIcon.jsp -->
