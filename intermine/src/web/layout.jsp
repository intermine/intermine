<?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-1-transitional.dtd">

<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<html:html locale="true" xhtml="true">
<head>
<html:base/>
<link rel="stylesheet" type="text/css" href="http://www.flymine.org/flymine.css" />
<meta content="microarray, bioinformatics, drosophila, genomics" name="keywords" />
<meta content="Integrated queryable database for Drosophila and Anopheles genomics" name="description" />
<meta content="text/html; charset=iso-8859-1" http-equiv="Content-Type" />
<title><tiles:getAsString name="title"/></title>
</head>

<body bgcolor="#f4eeff">

<table cellpadding="10" border="0" width="100%" align="center">
  <tr>
    <td valign="bottom" align="left" colspan="2"><tiles:insert attribute="header" /></td>
  </tr>
  <tr>
    <td valign="top" align="left" width="5%" height="10%" class="sidebar">
      <tiles:insert attribute='menu' />
    </td>
    <td rowspan="2" valign="top" align="left" class="main"> <tiles:insert attribute='body' /> </td>
  </tr>
  <tr>
    <td />
  </tr>
  <tr>
    <td colspan="2" align="center" class="footer"> <tiles:insert attribute="footer" /> </td>
  </tr>
</table>

</body>
</html:html>
