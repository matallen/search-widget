<div style="height:400px;">

<script src="//cdnjs.cloudflare.com/ajax/libs/list.js/1.5.0/list.min.js"></script>

<div id="hacker-list">
	<input class="search" />
	<!--
	<span class="sort" data-sort="name">Sort by name</span>
	-->
	<!--
	-->
	<table>
		<thead>
			<tr>
				<td><b>Offering</b></td>
				<td><b>Description</b></td>
				<td><b>Related Prods</b></td>
			</tr>
		</thead>
		<tbody id="template">
				<ul class="list"></ul>
		</tbody>
	</table>
	<!--
  <ul class="list"></ul>
  -->
</div>

<script>
  
  document.addEventListener("DOMContentLoaded", function() {
  	console.log("DOM Loaded");
  	loadData();
  });
  
  function loadData(){
  	var xhr = new XMLHttpRequest();
	  var ctx = "${pageContext.request.contextPath}";
	  //xhr.open("GET", "https://solutiontools.co.uk:8443/search-widget/api/search/grouped?filter=sso_searchable", true);
	  xhr.open("GET", "http://localhost:8082/search-widget/api/search/grouped2?filter=sso_searchable", true);
	  xhr.send();
	  xhr.onloadend = function () {
	    console.log("Got data rows");
	    values=JSON.parse(xhr.responseText);
	    loadGrid(values);
	  }
  }
  
  function loadGrid(values){
		var options = {
		  valueNames: [ 'offering', 'description', "documents" ],
		  //item: '<li><h3 class="offering"></h3><b>Description:</b> <span class="description"></span> - for this I need to parse the html<br/><br/><b>Related Solutions:</b> <span class="relatedSolutions"></span><br/><br/><b>Related Products:</b> Where do I get these? from the tags or parsing html?<span class="relatedProducts"></span><br/><p class="documents2"></p></li>'
		  //item: '<table><tr><td>Offering</td><td>Desc</td><td>related sols</td></tr></table>'
		  //item: '<tr><td><span class="offering"></span></td><td><span class="description"/></td></tr>'
		  //item: '<div><b><div class="offering"></div></b><div class="description"></div><div class="documents.id"></div></div>'
		  
		  template-engine: displayRow()
		};
		//$('template').list(values);
		
		var hackerList = new List('hacker-list', options, values);
		//var t=document.getElementById("template");
		//var hackerList = new List(t, options, values);
  }
  
  function displayRow(row){
  	return "asd";
  }
  
</script>


<style>
	#hacker-list input{
		border-radius: 25px;
	  padding: 7px 14px;
	  background-color: transparent;
	  border: solid 1px rgba(0, 0, 0, 0.2);
	  width: 200px;
	  box-sizing: border-box;
	  color: #2e2e2e;
	  margin-bottom: 5px;
	}
	#hacker-list{
	  font-family: Arial;
	}
</style>
</div>