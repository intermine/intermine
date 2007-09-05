<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- login.jsp -->
<html:xhtml/>
<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">
  <html:form action="/loginAction" focus="username" method="post" enctype="multipart/form-data">
    <fmt:message key="login.haspassword"/><br/><br/>
    <html:hidden property="returnToString"/>
    <table>
      <tr>
        <td><fmt:message key="login.username"/></td>
        <td><html:text property="username"/><br/></td>
      </tr>
      <tr>
        <td><fmt:message key="login.password"/></td>
        <td><html:password property="password"/><br/></td>
        <td><html:submit property="action"><fmt:message key="login.login"/></html:submit>
        &nbsp;&nbsp;<html:link action="/createAccount.do">...or create an account</html:link>
        </td>
      </tr>
    </table>
  </html:form>

<script language="javascript">
	var visibility = 'block';
	function toggleDiv(){
		document.getElementById('passwordDiv').style.display=visibility;
		if(visibility=='block') visibility='none';
		else visibility='block';
	}
</script>
  <br/>
<a href="javascript:toggleDiv();" >Forgotten password</a>
<div id="passwordDiv" style="display:none;padding-top:20px">
  <html:form action="/requestPasswordAction">  
    <fmt:message key="login.needspassword"/><br/><br/>
    <table>
      <tr>
        <td><fmt:message key="login.username"/></td>
        <td><html:text property="username"/></td>
        <td><html:submit property="action"><fmt:message key="login.passwordrequest"/></html:submit></td>
      </tr>
    </table>
  </html:form>
  <br/>
  
</div>
</im:boxarea>
<!-- /login.jsp -->
