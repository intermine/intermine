<!-- alignmentDisplayer.jsp -->
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<h3> Protein Family Alignment </h3>

<style>
  .seq-label { font-style: italic; font-weight: bold; text-align: center}
  .seq-piece { font-family: monospace; font-weight: normal; text-align: left; word-wrap:break-word;}
  .hmm-piece { font-family: monospace; font-weight: normal; text-align: left; word-wrap:break-word;}
</style>

<h1 class="seq-label"> ${primaryIdentifier} </h1>

<table>
    <tbody>
      <tr>
      <td class="seq-piece" >
      <div style="background:rgb(235,235,235); line-height: 1em">
                 ${fn:replace(fn:replace(alignment," ","&nbsp"),"\\n","<br>")} </pre>
      </td>
      </tr>
      <tr>
      <td class="hmm-piece" >
      <div style="background:rgb(235,235,235); line-height: 1em">
                 ${fn:replace(fn:replace(HMM," ","&nbsp"),"\\n","<br>")} </pre>
      </td>
      </tr>
    </tbody>
</table>
