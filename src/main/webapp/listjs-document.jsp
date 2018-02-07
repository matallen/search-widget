<div style="height:300px;">

<script src="//cdnjs.cloudflare.com/ajax/libs/list.js/1.5.0/list.min.js"></script>

<div id="hacker-list">
	<input class="search" />
	<!--
	<span class="sort" data-sort="name">Sort by name</span>
	-->
  <ul class="list"></ul>
</div>

<script>
  
  document.addEventListener("DOMContentLoaded", function() {
  	console.log("DOM Loaded");
  	loadData();
  });
  
  function loadData(){
  	var xhr = new XMLHttpRequest();
	  var ctx = "${pageContext.request.contextPath}";
	  xhr.open("GET", "https://solutiontools.co.uk:8443/search-widget/api/search/document?filter=tag(sso_searchable)", true);
	  xhr.send();
	  xhr.onloadend = function () {
	    console.log("Got data rows");
	    values=JSON.parse(xhr.responseText);
	    loadGrid(values);
	  }
  }
  
  function loadGrid(values){
		var options = {
		  valueNames: [ 'subject', 'description', {"name":"url", attr: "href"} ],
		  item: '<li><a href="" class="url"><h3 class="subject"></h3></a><p class="description"></p></li>'
		};
		
		var hackerList = new List('hacker-list', options, values);
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