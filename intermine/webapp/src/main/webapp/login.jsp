<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- login.jsp -->
<html:xhtml/>
<div class="body">

<h2>Account</h2>

<div id="login">
  <div class="plainbox">

    <div class="column">
      <html:form action="/loginAction" focus="username" method="post" enctype="multipart/form-data">
        <h3><fmt:message key="login.haspassword"/></h3>
        <html:hidden property="returnToString"/>
        <table>
          <tr>
            <td><fmt:message key="login.username"/></td>
            <td><html:text property="username"/><br/></td>
          </tr>
          <tr>
            <td><fmt:message key="login.password"/></td>
            <td><html:password property="password"/><br/></td>
          </tr>
          <tr>
            <td colspan="2"><html:submit property="action"><fmt:message key="login.login"/></html:submit></td>
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
      <a href="javascript:toggleDiv();" >Forgot password?</a>
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
    </div>

    <c:if test="${!empty OPENID_PROVIDERS && WEB_PROPERTIES['openid.allowed'] != 'false'}">
      <div class="column second">
        <im:debug message="${OPENID_PROVIDERS}"/>
        <h3 class="openid"><fmt:message key="login.openid"/></h3>
        <c:forEach var="provider" items="${OPENID_PROVIDERS}">
          <a class="<c:out value="${fn:toLowerCase(provider)}"/>"
          href="/${WEB_PROPERTIES['webapp.path']}/openid?provider=${provider}"></a>
        </c:forEach>
      </div>
    </c:if>

    <c:if test="${!empty OAUTH2_PROVIDERS && WEB_PROPERTIES['oauth2.allowed'] != 'false'}">
      <div class="column second oauth2"><im:debug message="${OAUTH2_PROVIDERS}"/>
      <h3 class="oauth"><fmt:message key="login.oauth2"/></h3>
        <c:forEach var="provider" items="${OAUTH2_PROVIDERS}">
          <c:choose>
            <c:when test="${fn:toLowerCase(provider) == 'elixir'}">
              <a class="elixir" href="/${WEB_PROPERTIES['webapp.path']}/oauth2authenticator.do?provider=${provider}">
                <img src="images/elixir-login.png"  alt="Log in with ELIXIR"/>
              </a>
           </c:when>
           <c:when test="${fn:toLowerCase(provider) == 'google'}">
             <a class="oauth2-button" href="/${WEB_PROPERTIES['webapp.path']}/oauth2authenticator.do?provider=${provider}">
               <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" height="20" width="20" class="icon-oauth">
                 <title>google</title>
                 <path d="M16.319 13.713v5.487h9.075c-0.369 2.356-2.744 6.9-9.075 6.9-5.463 0-9.919-4.525-9.919-10.1s4.456-10.1 9.919-10.1c3.106 0 5.188 1.325 6.375 2.469l4.344-4.181c-2.788-2.612-6.4-4.188-10.719-4.188-8.844 0-16 7.156-16 16s7.156 16 16 16c9.231 0 15.363-6.494 15.363-15.631 0-1.050-0.113-1.85-0.25-2.65l-15.113-0.006z"></path>
               </svg>               GOOGLE
             </a>
          </c:when>
          <c:when test="${fn:toLowerCase(provider) == 'im'}">
            <a class="oauth2-button" href="/${WEB_PROPERTIES['webapp.path']}/oauth2authenticator.do?provider=${provider}">
              <img class="icon-oauth" src="images/icons/intermine32x32.png" width="22" height="22" alt="Log in with InterMine" />
                IM
            </a>
         </c:when>
          <c:otherwise>
           <a class="oauth2-button"
            href="/${WEB_PROPERTIES['webapp.path']}/oauth2authenticator.do?provider=${provider}">
            <i class="fa fa-fw fa-<c:out value="${fn:toLowerCase(provider)}"/>"></i>
            ${provider}
           </a>
          </c:otherwise>
         </c:choose>
        </c:forEach>
      </div>
    </c:if>

    <div class="clear"></div>
  </div>
</div>

<div id="create">
  <div class="plainbox">
    <h3>Create account &hellip;</h3>

    <ul>
      <li>your <span class="lists">lists</span> and <span class="queries">queries</span> will be privately saved</li>
      <li>you can <span class="favorite">favorite</span> items</li>
      <li>you can share <span class="lists">lists</span> with other users</li>
    </ul>

    <br/>

    <html:link action="/createAccount.do">Create account now</html:link>
  </div>
</div>

<div class="clear"></div>
<!-- /login.jsp -->
