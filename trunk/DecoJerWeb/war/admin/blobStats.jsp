<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<jsp:useBean id="blobStats" class="org.decojer.web.controller.BlobStats" />
<% blobStats.calculateStats(); %>
<!DOCTYPE HTML>
<html lang="en-us">
<head>
<jsp:include page="/WEB-INF/template/head.jsp" />
</head>
<body>
	<jsp:include page="/WEB-INF/template/header.jsp" />
	<p>Blob Stats:</p>
	<p>Double: ${blobStats.doubleHashes}</p>
	<p>Number: ${blobStats.number}</p>
	<p>Size: ${blobStats.size}</p>
</body>
</html>