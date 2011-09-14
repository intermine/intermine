<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
  prefix="str"%>




<tiles:importAttribute />

<link rel="stylesheet" href="model/css/chromatin_states.css" type="text/css" media="screen" title="no title" charset="utf-8">

<html:xhtml />

<div class="body">

<h1>A genome-wide map of the chromatin landscape for Drosophila melanogaster</h1>
<br/>
<div style="font-size: 1.2em;">
<p>
Kharchenko at al., <a target ="new" href="http://dx.doi.org/10.1038/nature09725">Nature 471, pp. 480-485</a>
 produced a genome-wide map of 9 chromatin states for <i>D. melanogaster</i> in the S2 embryonic cell line the BG3 neuronal cell line.
</p>
</div>
<br/>
<p>This ideogram is based on a machine-learning approach which was used to identify the prevalent combinatorial patterns of 18 histone modifications across the genome. A simplified intensity-based model with nine states captures the overall complexity of chromatin patterns observed in S2 and BG3 cell lines, and we used this model to associate each genomic location with a particular combinatorial 'state', generating a chromatin-centric annotation of the genome (colour-coded as shown in the legend below).
</p>
<br/>

<h3>Detailed methods:</h3>
<p>
To derive a nine-state joint chromatin state model for the S2 and BG3 cells, the genome was first divided into 200 bp bins, and the average enrichment level was calculated within each bin based on unsmoothed M values (using all histone enrichment profiles and Pc to discount the genome-wide difference in S2 H3K27me3 profiles). The bin-average values of each mark were shifted by the genome-wide mean, scaled by the genome-wide variance, and quantile-normalized between the two cells. An HMM model with multivariate
normal emission distributions was generated using the data from both cell lines (30 seeding configurations determined with K-means clustering were used), and the Baum-Welch algorithm. States with minor intensity variations (Euclidian distance of mean emission values < 0.15) were merged. Larger models (up to 30 states) were examined, and the final number of states was chosen for optimal interpretability.
</p>
<br/>
<h3>Click on a region to open a detailed view in GBrowse:</h3>
<map name="Ideo_map">
    <area shape="rect" coords="32,6,45,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1;stop=450000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="46,6,59,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=480000;stop=930000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="60,6,73,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=960000;stop=1410000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="74,6,87,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1440000;stop=1890000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="88,6,101,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1920000;stop=2370000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="102,6,115,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2400000;stop=2850000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="116,6,129,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2880000;stop=3330000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="130,6,143,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3360000;stop=3810000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="144,6,157,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3840000;stop=4290000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="158,6,171,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=4320000;stop=4770000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="172,6,185,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=4800000;stop=5250000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="186,6,199,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=5280000;stop=5730000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="200,6,213,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=5760000;stop=6210000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="214,6,227,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=6240000;stop=6690000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="228,6,241,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=6720000;stop=7170000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="242,6,255,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=7200000;stop=7650000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="256,6,269,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=7680000;stop=8130000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="270,6,283,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=8160000;stop=8610000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="284,6,297,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=8640000;stop=9090000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="298,6,311,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=9120000;stop=9570000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="312,6,325,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=9600000;stop=10050000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="326,6,339,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=10080000;stop=10530000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="340,6,353,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=10560000;stop=11010000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="354,6,367,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11040000;stop=11490000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="368,6,381,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11520000;stop=11970000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="382,6,395,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12000000;stop=12450000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="396,6,409,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12480000;stop=12930000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="410,6,423,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12960000;stop=13410000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="424,6,437,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=13440000;stop=13890000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="438,6,451,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=13920000;stop=14370000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="452,6,465,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14400000;stop=14850000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="466,6,479,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14880000;stop=15330000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="480,6,493,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15360000;stop=15810000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="494,6,507,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15840000;stop=16290000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="508,6,521,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=16320000;stop=16770000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="522,6,535,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=16800000;stop=17250000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="536,6,549,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17280000;stop=17730000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="550,6,563,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17760000;stop=18210000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="564,6,577,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=18240000;stop=18690000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="578,6,591,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=18720000;stop=19170000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="592,6,605,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19200000;stop=19650000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="606,6,619,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19680000;stop=20130000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="620,6,633,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=20160000;stop=20610000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="634,6,647,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=20640000;stop=21090000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="648,6,661,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=21120000;stop=21570000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="662,6,675,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=21600000;stop=22050000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="676,6,689,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=22080000;stop=22530000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="690,6,703,32" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=22560000;stop=23010000;ref=2L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="32,46,45,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1;stop=500000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="46,46,59,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=540000;stop=1040000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="60,46,73,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1080000;stop=1580000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="74,46,87,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1620000;stop=2120000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="88,46,101,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2160000;stop=2660000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="102,46,115,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2700000;stop=3200000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="116,46,129,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3240000;stop=3740000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="130,46,143,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3780000;stop=4280000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="144,46,157,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=4320000;stop=4820000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="158,46,171,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=4860000;stop=5360000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="270,46,283,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=9180000;stop=9680000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="284,46,297,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=9720000;stop=10220000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="298,46,311,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=10260000;stop=10760000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="312,46,325,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=10800000;stop=11300000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="326,46,339,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11340000;stop=11840000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="340,46,353,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11880000;stop=12380000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="354,46,367,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12420000;stop=12920000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="368,46,381,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12960000;stop=13460000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="382,46,395,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=13500000;stop=14000000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="396,46,409,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14040000;stop=14540000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="410,46,423,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14580000;stop=15080000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="424,46,437,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15120000;stop=15620000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="438,46,451,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15660000;stop=16160000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="452,46,465,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=16200000;stop=16700000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="466,46,479,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=16740000;stop=17240000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="480,46,493,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17280000;stop=17780000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="494,46,507,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17820000;stop=18320000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="508,46,521,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=18360000;stop=18860000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="522,46,535,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=18900000;stop=19400000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="536,46,549,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19440000;stop=19940000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="550,46,563,72" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19980000;stop=20480000;ref=2R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="32,86,45,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1;stop=500000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="46,86,59,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=550000;stop=1050000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="60,86,73,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1100000;stop=1600000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="74,86,87,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1650000;stop=2150000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="88,86,101,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2200000;stop=2700000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="102,86,115,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2750000;stop=3250000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="116,86,129,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3300000;stop=3800000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="130,86,143,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3850000;stop=4350000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="144,86,157,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=4400000;stop=4900000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="158,86,171,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=4950000;stop=5450000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="172,86,185,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=5500000;stop=6000000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="186,86,199,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=6050000;stop=6550000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="200,86,213,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=6600000;stop=7100000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="214,86,227,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=7150000;stop=7650000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="228,86,241,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=7700000;stop=8200000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="242,86,255,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=8250000;stop=8750000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="256,86,269,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=8800000;stop=9300000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="270,86,283,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=9350000;stop=9850000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="284,86,297,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=9900000;stop=10400000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="298,86,311,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=10450000;stop=10950000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="312,86,325,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11000000;stop=11500000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="326,86,339,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11550000;stop=12050000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="340,86,353,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12100000;stop=12600000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="354,86,367,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12650000;stop=13150000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="368,86,381,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=13200000;stop=13700000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="382,86,395,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=13750000;stop=14250000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="396,86,409,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14300000;stop=14800000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="410,86,423,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14850000;stop=15350000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="424,86,437,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15400000;stop=15900000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="438,86,451,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15950000;stop=16450000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="452,86,465,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=16500000;stop=17000000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="466,86,479,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17050000;stop=17550000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="480,86,493,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17600000;stop=18100000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="494,86,507,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=18150000;stop=18650000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="508,86,521,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=18700000;stop=19200000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="522,86,535,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19250000;stop=19750000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="536,86,549,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19800000;stop=20300000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="550,86,563,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=20350000;stop=20850000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="564,86,577,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=20900000;stop=21400000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="578,86,591,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=21450000;stop=21950000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="592,86,605,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=22000000;stop=22500000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="606,86,619,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=22550000;stop=23050000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="620,86,633,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=23100000;stop=23600000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="634,86,644,112" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=23650000;stop=24150000;ref=3L;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="32,126,45,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1;stop=500000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="46,126,59,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=560000;stop=1060000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="60,126,73,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1120000;stop=1620000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="74,126,87,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1680000;stop=2180000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="88,126,101,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2240000;stop=2740000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="102,126,115,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2800000;stop=3300000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="116,126,129,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3360000;stop=3860000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="130,126,143,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3920000;stop=4420000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="144,126,157,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=4480000;stop=4980000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="158,126,171,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=5040000;stop=5540000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="172,126,185,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=5600000;stop=6100000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="186,126,199,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=6160000;stop=6660000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="200,126,213,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=6720000;stop=7220000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="214,126,227,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=7280000;stop=7780000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="228,126,241,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=7840000;stop=8340000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="242,126,255,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=8400000;stop=8900000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="256,126,269,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=8960000;stop=9460000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="270,126,283,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=9520000;stop=10020000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="284,126,297,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=10080000;stop=10580000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="298,126,311,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=10640000;stop=11140000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="312,126,325,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11200000;stop=11700000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="326,126,339,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11760000;stop=12260000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="340,126,353,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12320000;stop=12820000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="354,126,367,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12880000;stop=13380000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="368,126,381,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=13440000;stop=13940000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="382,126,395,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14000000;stop=14500000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="396,126,409,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14560000;stop=15060000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="410,126,423,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15120000;stop=15620000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="424,126,437,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15680000;stop=16180000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="438,126,451,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=16240000;stop=16740000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="452,126,465,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=16800000;stop=17300000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="466,126,479,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17360000;stop=17860000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="480,126,493,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17920000;stop=18420000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="494,126,507,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=18480000;stop=18980000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="508,126,521,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19040000;stop=19540000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="522,126,535,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19600000;stop=20100000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="536,126,549,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=20160000;stop=20660000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="550,126,563,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=20720000;stop=21220000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="564,126,577,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=21280000;stop=21780000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="578,126,591,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=21840000;stop=22340000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="592,126,605,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=22400000;stop=22900000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="606,126,619,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=22960000;stop=23460000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="620,126,633,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=23520000;stop=24020000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="634,126,647,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=24080000;stop=24580000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="648,126,661,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=24640000;stop=25140000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="662,126,675,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=25200000;stop=25700000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="676,126,689,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=25760000;stop=26260000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="690,126,703,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=26320000;stop=26820000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="704,126,717,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=26880000;stop=27380000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="718,126,728,152" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=27440000;stop=27900000;ref=3R;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="32,166,38,192" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1;stop=250000;ref=4;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="39,166,45,192" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=350000;stop=600000;ref=4;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="46,166,52,192" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=700000;stop=950000;ref=4;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="53,166,59,192" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1050000;stop=1300000;ref=4;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="32,206,45,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1;stop=500000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="46,206,59,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=570000;stop=1070000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="60,206,73,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1140000;stop=1640000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="74,206,87,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=1710000;stop=2210000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="88,206,101,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2280000;stop=2780000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="102,206,115,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=2850000;stop=3350000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="116,206,129,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3420000;stop=3920000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="130,206,143,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=3990000;stop=4490000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="144,206,157,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=4560000;stop=5060000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="158,206,171,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=5130000;stop=5630000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="172,206,185,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=5700000;stop=6200000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="186,206,199,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=6270000;stop=6770000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="200,206,213,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=6840000;stop=7340000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="214,206,227,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=7410000;stop=7910000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="228,206,241,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=7980000;stop=8480000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="242,206,255,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=8550000;stop=9050000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="256,206,269,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=9120000;stop=9620000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="270,206,283,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=9690000;stop=10190000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="284,206,297,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=10260000;stop=10760000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="298,206,311,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=10830000;stop=11330000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="312,206,325,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11400000;stop=11900000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="326,206,339,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=11970000;stop=12470000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="340,206,353,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=12540000;stop=13040000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="354,206,367,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=13110000;stop=13610000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="368,206,381,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=13680000;stop=14180000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="382,206,395,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14250000;stop=14750000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="396,206,409,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=14820000;stop=15320000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="410,206,423,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15390000;stop=15890000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="424,206,437,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=15960000;stop=16460000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="438,206,451,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=16530000;stop=17030000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="452,206,465,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17100000;stop=17600000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="466,206,479,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=17670000;stop=18170000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="480,206,493,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=18240000;stop=18740000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">

    <area shape="rect" coords="494,206,507,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=18810000;stop=19310000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="508,206,521,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19380000;stop=19880000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="522,206,535,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=19950000;stop=20450000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="536,206,549,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=20520000;stop=21020000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="550,206,563,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=21090000;stop=21590000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="564,206,577,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=21660000;stop=22160000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
    <area shape="rect" coords="578,206,590,231" href=http://modencode.oicr.on.ca/fgb2/gbrowse/fly/?start=22230000;stop=22400000;ref=X;grid=on;l=Genes%1E9STATE_S2%1E9STATE_BG3 target="_blank">
</map>
<img src="model/images/fly_all_chrs_ideogram.png" usemap="#Ideo_map" border=0 alt="modENCODE CS Ideogram"/>
<br/>


<div>
<h3>9-state Chromatin legend:</h3>
<table class="legend">
  <tr><th>State</th><th>Description</th><th>Color</th></tr>
    <tr><td>1</td><td>Promoter and TSS</td><td style="background-color: red"></td></tr>
    <tr><td>2</td><td>Transcription elongation</td><td style="background-color: purple"></td></tr>
    <tr><td>3</td><td>Regulatory regions (enhancers)</td><td style="background-color: brown"></td></tr>
    <tr><td>4</td><td>Active introns</td><td style="background-color: coral"></td></tr>
    <tr><td>5</td><td>Active genes on the Male X</td><td style="background-color: green"></td></tr>
    <tr><td>6</td><td>Polycomb-mediated repression</td><td style="background-color: grey"></td></tr>
    <tr><td>7</td><td>Pericentromeric heterochromatin</td><td style="background-color: darkblue"></td></tr>
    <tr><td>8</td><td>Heterochromatin-like embedded in euchromatin</td><td style="background-color: lightblue"></td></tr>
    <tr><td>9</td><td>Transcriptionally silent, intergenic</td><td style="background-color: lightgrey"></td></tr>
</table>
</div>

<br/>
<h3>See also the Park lab's viewer for these data:</h3>
<br/>
<a target ="new" href="http://compbio.med.harvard.edu/flychromatin/"><div class="heatmap"><img src="themes/modmine/parklab.jpg" alt="Park Lab Viewer"/></div></a>


</div>


