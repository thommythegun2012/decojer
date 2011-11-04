<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
	import="org.decojer.web.util.*"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<jsp:useBean id="maven" class="org.decojer.web.controller.Maven" />
<!DOCTYPE HTML>
<html lang="en-us">
<head>
<jsp:include page="/WEB-INF/template/head.jsp" />
</head>
<body>
	<jsp:include page="/WEB-INF/template/header.jsp" />
	<h1>DecoJer - A Java Decompiler</h1>
	<p>Merge:</p>
	<p>
		${maven.rss}
	</p>
</body>
</html>