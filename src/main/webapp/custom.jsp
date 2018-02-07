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
  	var list=document.getElementById("myUL");
  	for (var i=0;i<values.length;i++){
  		var newEntry=document.createElement("li");
  		//newEntry.appendChild(document.createTextNode(createRow(values[i])));
  		newEntry.innerHTML=createRow(values[i]);
  		list.appendChild(newEntry);
  	}
  }
  
  function createRow(row){
  //alert(JSON.stringify(row));
  	//return row['offering'];
  	//return "<a href='#'>"+row['offering']+"</a>";
  	//return template;
  	var result=""+
			"<table><tr>"+
			"		<td class='offering'>"+row['offering']+"<br/><span class='description'>"+row['description']+"</span></td>"+
			"		<td rowspan='2' style='vertical-align:top;'>";
		
		var files="";
		for(var i=0;i<row['documents'].length;i++){
			files+="<a href='"+row['documents'][i].url+"'>"+row['documents'][i].name+"</a>";
		}
		result+=files;
		
		result+="</td>"+
			"	</tr><!--tr style='vertical-align:top;'>"+
			"		<td class='description'>"+row['description']+"</td>"+
			"	</tr-->"+
			"</table>";
		
		return result;
  }


	function filterFunc() {
	    // Declare variables
	    var input, filter, ul, li, a, i;
	    input = document.getElementById('myInput');
	    filter = input.value.toUpperCase();
	    ul = document.getElementById("myUL");
	    li = ul.getElementsByTagName('li');
	
	    // Loop through all list items, and hide those who don't match the search query
	    for (i = 0; i < li.length; i++) {
	        a = li[i].getElementsByTagName("a")[0];
	        if (a.innerHTML.toUpperCase().indexOf(filter) > -1) {
	            li[i].style.display = "";
	        } else {
	            li[i].style.display = "none";
	        }
	    }
	}
</script>

<style>
	#myInput {
	    //background-image: url('/css/searchicon.png'); /* Add a search icon to input */
	    background-position: 10px 12px; /* Position the search icon */
	    background-repeat: no-repeat; /* Do not repeat the icon image */
	    width: 100%; /* Full-width */
	    font-size: 16px; /* Increase font-size */
	    padding: 12px 20px 12px 40px; /* Add some padding */
	    border: 1px solid #ddd; /* Add a grey border */
	    margin-bottom: 12px; /* Add some space below the input */
	}
	#myUL {
	    /* Remove default list styling */
	    list-style-type: none;
	    padding: 0;
	    margin: 0;
	}
	#myUL li a {
	    border: 1px solid #ddd; /* Add a border to all links */
	    margin-top: -1px; /* Prevent double borders */
	    background-color: #f6f6f6; /* Grey background color */
	    padding: 12px; /* Add some padding */
	    text-decoration: none; /* Remove default text underline */
	    font-size: 18px; /* Increase the font-size */
	    color: black; /* Add a black text color */
	    display: block; /* Make it into a block element to fill the whole list */
	}
	.offering {
	    border: 1px solid #ddd; /* Add a border to all links */
	    margin-top: -1px; /* Prevent double borders */
	    background-color: #f6f6f6; /* Grey background color */
	    padding: 12px; /* Add some padding */
	    text-decoration: none; /* Remove default text underline */
	    font-size: 18px; /* Increase the font-size */
	    color: black; /* Add a black text color */
	    display: block; /* Make it into a block element to fill the whole list */
	}
	.description {
	    margin-top: -1px; /* Prevent double borders */
	    background-color: #f6f6f6; /* Grey background color */
	    padding: 12px; /* Add some padding */
	    text-decoration: none; /* Remove default text underline */
	    font-size: 14px; /* Increase the font-size */
	    color: black; /* Add a black text color */
	    display: block; /* Make it into a block element to fill the whole list */
	}
	#myUL li a:hover:not(.header) {
	    background-color: #eee; /* Add a hover effect to all links, except for headers */
	}
</style>

<input type="text" id="myInput" onkeyup="filterFunc()" placeholder="Search for names..">

<ul id="myUL">
</ul>