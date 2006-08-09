<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top" rowspan="2">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <DL>
          <DT><A href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&cmd=Retrieve&dopt=AbstractPlus&list_uids=10877382&query_hl=1&itool=pubmed_docsum"></A></DT>
          <DD>
            Milk literature is retrieved from three main scientific literature sources: PubMed, OVID and Web of Science. Articles are retieved via ontology searching and milk protein sentences are extracted. These sentences are then analysed to determine and tag biological concepts within them. This process makes use of a customised subset of the UMLS (<a href="http://umlsks.nlm.nih.gov/kss/servlet/Turbine/template/admin,user,KSS_login.vm">Unified Medical Language System</a>) which has been enhanced for terminology from the domains of milk, milk proteins, lactation and colostrum. Relationships can then be tracked between milk-related biological concepts and thus novel information can be extracted.
          </DD>
        </DL>
      </div>
    </td>
  </tr>
</table>
