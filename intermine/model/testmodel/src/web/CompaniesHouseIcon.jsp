<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<%-- Display an icon linked to companys house --%>

<html:link href="http://www.companieshouse.gov.uk/info/do_search.cgi?cname=${object.name}">
  <html:img src="/model/companiesHouse.ico"/>
</html:link>
