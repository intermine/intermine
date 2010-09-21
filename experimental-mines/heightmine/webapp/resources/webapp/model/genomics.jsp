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
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download
      </div>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <div class="body">

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv1');">
            <img id='hiddenDiv1Toggle' src="images/disclosed.gif" title="Click here to view the datasets" />
            Major data sets ...
          </a>
        </h4>

        <div id="hiddenDiv1" class="dataSetDescription">
          <p>
            <a href="/">HeightMine</a> is a resource aimed at the <i>Homo Sapiens</i> and
            <i>Mus Musculus</i> research communities hence the focus is on those organisms.
          </p>
         <ul>
        </ul>
        </div>

        <h4>
          <a href="javascript:toggleDiv('hiddenDiv2');">
            <img id='hiddenDiv2Toggle' src="images/disclosed.gif" title="Click here to view more datasets"/>
            Minor data sets ...
          </a>
        </h4>

      </div>
   </td>

    <td width="40%" valign="top">
      <div class="body">
        <ul>

          <li>
            <im:querylink text="All <i><Homo Sapiens/i> gene identifiers" skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" sortOrder="Gene.primaryIdentifier asc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.name" type="String">
    <constraint op="=" value="Homo Sapiens" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>

          <li>
            <im:querylink text="All <i>Mus Musculus</i> gene identifiers and chromosomal positions " skipBuilder="true">
<query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.start Gene.chromosomeLocation.end" sortOrder="Gene.primaryIdentifier asc">
  <node path="Gene" type="Gene">
  </node>
  <node path="Gene.organism" type="Organism">
  </node>
  <node path="Gene.organism.name" type="String">
    <constraint op="=" value="Mus Musculus" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
            </im:querylink>
          </li>

        </ul>
      </div>
    </td>
  </tr>

</table>
