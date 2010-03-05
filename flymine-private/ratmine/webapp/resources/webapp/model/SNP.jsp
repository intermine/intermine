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
		 <!--insert hidden here -->
		<h4>
			<a href="javascript:toggleDiv('hiddenDiv1');">
			<img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
			SNPs in RatMine ...
			</a>
		</h4>

		<div id="hiddenDiv1" class="dataSetDescription">
			<p>
			<a href="/">RatMine</a> contains <i>Rattus Norvegicus</i> SNP
			data from:
			</p>
			<ul>
				<li>
				<a href="http://www.ensembl.org/Rattus_norvegicus/Info/Index">
				Ensembl
				</a>
				</li>
			</ul>
		</div>
      </div>
    </td>
    <td width="40%" valign="top">
      <div class="body">
        <ul>
          <li>
            <im:querylink text="All <i>Rattus norvegicus</i> SNPs (browse)" skipBuilder="true">
				<query name="" model="genomic" view="SNP.primaryIdentifier SNP.allele2 SNP.allele1 SNP:chromosome.primaryIdentifier SNP:chromosomeLocation.start SNP:chromosomeLocation.end" sortOrder="SNP.primaryIdentifier asc">
				</query>
            </im:querylink>
          </li>
        </ul>
      </div>
    </td>
  </tr>
</table>