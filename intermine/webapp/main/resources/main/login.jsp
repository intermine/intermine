<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- login.jsp -->
<html:xhtml/>
<div class="body">
  <html:form action="/loginAction">
    <fmt:message key="login.haspassword"/><br/><br/>
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
<a href="javascript:toggleDiv();" >Change password / Forgotten password</a>
</div>

<div id="passwordDiv" style="display:none;">
  <im:box titleKey="login.passwordrequest">
  <div class="body">
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
  </div>
  </im:box>
  <br/>

  <im:box titleKey="password.changepassword">
  <div class="body">
  <html:form action="/changePasswordAction">  
    <table>
      <tr>
        <td><fmt:message key="password.username"/></td>
        <td><html:text property="username"/></td>
      </tr>
      <tr>
        <td><fmt:message key="password.oldpassword"/></td>
        <td><html:password property="oldpassword"/></td>
      </tr>
      <tr>
        <td><fmt:message key="password.newpassword"/></td>
        <td><html:password property="newpassword"/></td>
      </tr>
      <tr>
        <td><fmt:message key="password.newpassword2"/></td>
        <td><html:password property="newpassword2"/></td>
        <td><html:submit property="action"><fmt:message key="password.passwordchange"/></html:submit></td>
      </tr>
    </table>
  </html:form>
  </div>
  </im:box>
</div>
<!-- /login.jsp -->
