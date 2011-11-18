<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<jsp:useBean id="maven" class="org.decojer.web.controller.Maven" />
<%
	maven.importCentralRss();
%>
<!DOCTYPE HTML>
<html lang="en-us">
<head>
<jsp:include page="/WEB-INF/template/head.jsp" />
</head>
<body>
	<jsp:include page="/WEB-INF/template/header.jsp" />
	<h2>Import from Maven Central RSS</h2>
	<p>Imported: ${maven.importedArtifacts}</p>
</body>
</html>