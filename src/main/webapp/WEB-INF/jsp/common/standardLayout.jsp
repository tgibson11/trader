<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
	<head>
	    <title>Trader</title>
	    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	    <link rel="stylesheet" type="text/css" href="css/trader.css" />
        <script src="//ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js"></script>
        <script src="//cdn.datatables.net/1.10.0/js/jquery.dataTables.min.js"></script>
        <script src="//ajax.googleapis.com/ajax/libs/dojo/1.6/dojo/dojo.xd.js"></script>
	</head>
	<body>
		<div id="header">	
		    <ul>
		        <li id="home-tab"><a href="<c:url value="home" />">Home</a></li>
		        <li id="performance-tab"><a href="<c:url value="performance" />">Performance</a></li>
		        <li id="log-tab"><a href="<c:url value="log" />">Log</a></li>
		    </ul>	
		</div>        
        <div id="content">
            <tiles:insertAttribute name="body" />
        </div>
		<script>
			$(function() {
				var selectedClassName = "selected";
				var path = window.location.pathname;
				if (path.indexOf("home") != -1) {
					$("#home-tab").addClass(selectedClassName);
				} else if (path.indexOf("performance") != -1) {
					$("#performance-tab").addClass(selectedClassName);
				} else if (path.indexOf("log") != -1) {
					$("#log-tab").addClass(selectedClassName);
				}
			});
		</script>
	</body>
</html>
