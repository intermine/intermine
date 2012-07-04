<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- tracks.jsp -->

<html:xhtml/>

<link rel="stylesheet" href="css/resultstables.css" type="text/css" />
<link rel="stylesheet" href="css/toolbar.css" type="text/css" media="screen" title="Toolbar Style" charset="utf-8"/>

<div class="body">
<h3>Many user actions are recorded to track usage over time.</h3>
<i>Note that these totals don't include web service requests.</i>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="Template usage"/>
    <tiles:put name="description" value="Number of template query executions."/>
    <tiles:put name="id" value="template"/>
    <tiles:put name="mainColumn" value="Template"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="Custom queries executed"/>
    <tiles:put name="description" value="Each time a query is run from the QueryBuilder."/>
    <tiles:put name="id" value="query"/>
    <tiles:put name="mainColumn" value="Query Type"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="Logins"/>
    <tiles:put name="description" value="Tracks each time a user logs into their account."/>
    <tiles:put name="id" value="login"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="Keyword Searches"/>
    <tiles:put name="description" value="Number of keyword searches made."/>
    <tiles:put name="id" value="search"/>
    <tiles:put name="mainColumn" value="Keyword"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="List Analysis page views"/>
    <tiles:put name="description" value="List Analysis page views, not including when a list is initially created."/>
    <tiles:put name="id" value="listExecution"/>
    <tiles:put name="mainColumn" value="List Type"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="List Creation"/>
    <tiles:put name="description" value="Number of lists created by upload or from results tables."/>
    <tiles:put name="id" value="listCreation"/>
    <tiles:put name="mainColumn" value="List Type"/>
</tiles:insert>
</div>  <!-- whole page body -->
<!-- /tracks.jsp -->