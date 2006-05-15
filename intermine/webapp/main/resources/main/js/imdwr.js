function setFavouriteTemplate(templateName, image){
	AjaxServices.setFavouriteTemplate(templateName);
	image.src='images/star_active.gif';
	image.onclick='';
	image.style.cursor='';
	image.title='This template is a favourite';
}

function precomputeTemplate(templateName){
	document.getElementById('precompute_'+templateName).innerHTML="Precomputing..";
	AjaxServices.preCompute(templateName,function(str) { 
		document.getElementById('precompute_'+templateName).style.color="#777";
		document.getElementById('precompute_'+templateName).innerHTML="Precomputed";
	 });
}