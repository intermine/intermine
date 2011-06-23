<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>




<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Current data
      </div>
      <div class="body">


          <p>A mapping between genes and publications for <i>D. melanogaster</i> and
            <i>C. elegans</i> from NCBI, FlyBase and WormBase.
</p>

      </div>
    </dl>


    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
        <ul>

          <li>
           <im:querylink text="All <i> D. melanogaster</i> publications"  skipBuilder="true">
<query name="" model="genomic" view="Gene.publications.year Gene.publications.firstAuthor Gene.publications.journal Gene.publications.title Gene.publications.pubMedId" sortOrder="Gene.publications.year asc">
    <constraint path="Gene.organism.shortName" op="=" value="D. melanogaster"/>
</query>
       </im:querylink>
          </li>
<!--
          <li>
           <im:querylink text="All <i> D. pseudoobscura</i> publications"  skipBuilder="true">
<query name="" model="genomic" view="Gene.publications.year Gene.publications.firstAuthor Gene.publications.journal Gene.publications.title Gene.publications.pubMedId" sortOrder="Gene.publications.year asc">
  <constraint path="Gene.organism.shortName" op="=" value="D. pseudoobscura"/>
</query>
       </im:querylink>
          </li>
-->
          <li>
           <im:querylink text="All <i> C. elegans</i> publications"  skipBuilder="true">
<query name="" model="genomic" view="Gene.publications.year Gene.publications.firstAuthor Gene.publications.journal Gene.publications.title Gene.publications.pubMedId" sortOrder="Gene.publications.year asc">
    <constraint path="Gene.organism.shortName" op="=" value="C. elegans"/>
</query>
           </im:querylink>
          </li>

        </ul>
      </div>
    </td>

  </tr>
</table>

