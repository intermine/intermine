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
          <DT>Review articles</DT>
          <DD>
            All peptides from the following review articles have been loaded. Bioactive peptides are manually curated from the milk literature, with original references, and they are linked to their parent protein (as described in the reference).
            <UL>
              <LI><I>Fox et al, 2003</I></li>
	      <LI><I><A href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&cmd=Retrieve&dopt=AbstractPlus&list_uids=10877382&query_hl=1&itool=pubmed_docsum">Clare and Swaisgood, 2000</a><I></li>
              <LI><I>Korhonen and Pihlanto, 2006</I></li>
            </UL>
          </DD>
        </DL>
      </div>
    </td>
  </tr>
</table>
