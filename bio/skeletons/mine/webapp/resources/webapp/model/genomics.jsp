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
          <a href="/mousemine">MouseMine</a> contains genomic data for the laboratory mouse.
        </p>
        <ul>
          <li>
            <a href="http://www.informatics.jax.org/">
              Mouse genome features, nomenclature, functional annotations from MGI.
	      </a>
          </li>
        </ul>
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All mouse Genes (identifiers, symbols, genomic coordinates" skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.organism.shortName Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" sortOrder="Gene.primaryIdentifier asc">
<constraint path="Gene.organism.shortName" op="=" value="M. musculus"/>
</query>
            </im:querylink>
          </li>
        </ul>
      </div>
    </td>
  </tr>
</table>
