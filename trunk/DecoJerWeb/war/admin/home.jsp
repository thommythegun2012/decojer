<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE HTML>
<html lang="en-us">
<head>
<jsp:include page="/WEB-INF/template/head.jsp" />
</head>
<body>
	<jsp:include page="/WEB-INF/template/header.jsp" />
	<ul>
		<li><a href="/admin/blobStats.jsp">Blob Stats</a></li>
		<li><a href="/admin/pomStats.jsp">POM Stats</a></li>
		<li><a href="/admin/merge.jsp">Merge Test</a></li>
		<li><a href="/admin/importCentralRss.jsp">Maven Import
				Central RSS</a></li>
		<li><a href="/admin/importCentralAll.jsp">Maven Import
				Central All</a></li>
		<li><a href="/admin/urlFetch.jsp">URL Fetch</a></li>
	</ul>
</body>
</html>