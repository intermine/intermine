<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top" rowspan="2">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <DL>
          <DT>Phytozome Families </DT>
          <DD>
            All proteins from the Phytozome database
            are grouped into a hierachical structure of related polypeptide sequence. A family
            consisting of more than one protein also has a computed multiple sequence alignment (MSA)
            and computed centroid sequence.
          </DD>
        </DL>
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download data
      </div>
      <div class="body">
        <ul>
          <li>
            <span style="white-space:nowrap">
              <im:querylink text="A bulk query for Protein Families:" skipBuilder="true">
<query name="" model="genomic" view="ProteinFamily.clusterId" sortOrder="ProteinFamily.clusterId asc">
  <constraint path="ProteinFamily.clusterId" op="=" value="47396511"/>
</query>
              </im:querylink>
            </span>
          </li>
        </ul>
      </div>
    </td>
  </tr>
</table>
