<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top">
      <div class="heading2">
        Data sets
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download
      </div>
    </td>
  </tr>
  <tr>
    <td>
      <div class="body">
        <p>
          <a href="/malariamine">MalariaMine</a> contains <i>Plasmodium falciparum 3D7</i> genome
          data from:
        </p>
        <ul>
          <li>
            <a href="http://www.genedb.org/">
              Fasta sequences for <i>P. falciparum 3D7</i></a>
          </li>
          <li>
            <a href="http://www.genedb.org/">
              GFF3 for <i>P. falciparum 3D7</i> genome features</a>
          </li>
        </ul>
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All <i>P. falciparum 3D7</i> genes identifiers, chromosome positions and chromosome identifiers" skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.organism.shortName Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" sortOrder="Gene.primaryIdentifier asc">
</query>
            </im:querylink>
          </li>
        </ul>
      </div>
    </td>
  </tr>
</table>
