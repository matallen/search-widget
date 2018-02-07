<html>
<head>
<style> html, body { margin: 0; padding: 0; height: 100%; } </style>
    <script src="https://unpkg.com/ag-grid@16.0.0/dist/ag-grid.min.js"></script>
</head>
<body>

<div id="myGrid" style="height: 115px;width:500px;" class="ag-theme-fresh"></div>

<script>
  var rowData;
  var xhr = new XMLHttpRequest();
  var ctx = "${pageContext.request.contextPath}";
  xhr.open("GET", "https://solutiontools.co.uk:8443/search-widget/api/search/grouped?filter=sso_searchable", true);
  xhr.send();
  xhr.onloadend = function () {
    console.log("Got data rows");
    rowData=JSON.parse(xhr.responseText);
    loadGrid();
  }
  
  function loadGrid(){
    console.log("Loading grid");
		// specify the columns
		var columnDefs = [
		    {headerName: "Offering", field: "offering"},
		    {headerName: "Description", field: "description"},
		    {headerName: "Links", field: "documents2"}
		];
		
		// let the grid know which columns and what data to use
		var gridOptions = {
		    columnDefs: columnDefs,
		    rowData: rowData,
		    onGridReady: function (params) {
		        params.api.sizeColumnsToFit();
		    }
		};
		
		document.addEventListener("DOMContentLoaded", function() {
		
		    // lookup the container we want the Grid to use
		    var eGridDiv = document.querySelector('#myGrid');
		
		    // create the grid passing in the div to use together with the columns & data we want to use
		    new agGrid.Grid(eGridDiv, gridOptions);
		});
		
	}
</script>
</body>
</html>