<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- manageApiKey.jsp -->
<html:xhtml/>
&nbsp;

<h1><fmt:message key="user.accountdetails.heading"/></h2>

<h2><fmt:message key="user.preferences.heading"/></h2>

<p class="userprefs">
  <fmt:message key="user.preferences.description"/>
</p>

<c:set var="preferences" value="${PROFILE.preferences}" scope="request"/>

<div class="collection-table">
    <h3><fmt:message key="user.preferences.current"/></h3>
    <table class="user-preferences">
        <thead>
            <tr>
                <th>Key</th>
                <th>Value</th>
            </tr>
        </thead>
        <tbody>
           <c:forEach var="key" items="${SPECIAL_PREFERENCES}">
               <tr>
                    <td><fmt:message key="user.preferences.${key}"/></td>
                    <td>
                        <form class="editable-user-preference">
                            <c:choose>
                              <c:when test="${imf:contains(BOOLEAN_PREFERENCES, key)}">
                                <input type="checkbox" name="${key}" ${imf:containsKey(preferences, key) ? "" : "checked"}>
                              </c:when>
                              <c:otherwise>
                                <input type="text" name="${key}" value="${preferences[key]}"/>
                                <button class="update">Update</button>
                                <button class="unset">Unset</button>
                              </c:otherwise>
                            </c:choose>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${PROFILE.superuser || WEB_PROPERTIES['custom.preferences.allowed'] }">
              <c:forEach var="entry" items="${preferences}">
                <c:if test="${!imf:contains(SPECIAL_PREFERENCES, entry.key)}">
                  <tr>
                      <td><c:out value="${entry.key}"/></td>
                      <td>
                          <form class="editable-user-preference">
                              <input type="text" name="${entry.key}" value="${entry.value}"/>
                              <button class="update">Update</button>
                              <button class="delete">Delete</button>
                          </form>
                      </td>
                  </tr>
                </c:if>
              </c:forEach>
            </c:if>
        </tbody>
    </table>
</div>

<c:if test="${PROFILE.superuser || WEB_PROPERTIES['custom.preferences.allowed'] }">
  <button class="add-user-preference">Add new preference</button>
  <form class="add-user-preference">
    <input class="prefname" name="key" type="text" placeholder="name">
    <span>&nbsp;=&nbsp;</span>
    <input class="prefvalue" name="value" type="text" placeholder="value">
    <br/>
    <button type="submit">Save</button>
    <button class="cancel">Cancel</button>
  </form>
</c:if>

<h2><fmt:message key="apikey.heading"/></h2>

<p class="apikey"><fmt:message key="apikey.description"/></p>

<div class="apikey">
<c:choose>
    <c:when test="${empty PROFILE.apiKey}">
        <span class="apikey nokey">
            <fmt:message key="apikey.nokey"/>
        </span>
    </c:when>
    <c:otherwise>
        <span class="apikey">
            <c:out value="${PROFILE.apiKey}"/>
        </span>
    </c:otherwise>
</c:choose>
</div>

<button id="newApiKeyButton">
    <fmt:message key="apikey.generate"/>
</button>

<c:if test="${empty PROFILE.apiKey}">
  <c:set var="delete_css" value="display: none;"/>
</c:if>

<button id="deleteApiKeyButton" style="${delete_css}">
  <fmt:message key="apikey.deleteKey"/>
</button>

<script type="text/javascript">
$CURRENT_USER="${PROFILE.username}";
</script>

<!-- /managerApiKey.jsp -->
