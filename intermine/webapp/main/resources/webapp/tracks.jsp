<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- tracks.jsp -->

<html:xhtml/>

<link rel="stylesheet" href="css/resultstables.css" type="text/css" />
<link rel="stylesheet" href="css/toolbar.css" type="text/css" media="screen" title="Toolbar Style" charset="utf-8"/>

<div class="body">
<tiles:insert name="track.tile">
    <tiles:put name="title" value="Template Tracks"/>
    <tiles:put name="id" value="template"/>
    <tiles:put name="mainColumn" value="Template"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="Query Tracks"/>
    <tiles:put name="id" value="query"/>
    <tiles:put name="mainColumn" value="Query Type"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="Login Tracks"/>
    <tiles:put name="id" value="login"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="Keyword Search Tracks"/>
    <tiles:put name="id" value="search"/>
    <tiles:put name="mainColumn" value="Keyword"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="List Execution Tracks"/>
    <tiles:put name="id" value="listExecution"/>
    <tiles:put name="mainColumn" value="List Type"/>
</tiles:insert>
<tiles:insert name="track.tile">
    <tiles:put name="title" value="List Creation Tracks"/>
    <tiles:put name="id" value="listCreation"/>
    <tiles:put name="mainColumn" value="List Type"/>
</tiles:insert>
</div>  <!-- whole page body -->
<!-- /tracks.jsp -->