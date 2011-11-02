<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
	import="org.decojer.web.util.*"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<jsp:useBean id="merge" class="org.decojer.web.controller.Merge" />
<jsp:setProperty name="merge" property="*" />
<!DOCTYPE HTML>
<html lang="en-us">
<head>
<jsp:include page="/WEB-INF/template/head.jsp" />
</head>
<body>
	<jsp:include page="/WEB-INF/template/header.jsp" />
	<h1>DecoJer - A Java Decompiler</h1>
	<p>Merge:</p>
	<form>
		<input type="text" name="type1" value="${merge.type1}"><input
			type="text" name="type2" value="${merge.type2}"><input
			type="submit" value="Submit">
	</form>
	<p>
		<c:out value="${merge.mergedType}" escapeXml="true" />
	</p>
</body>
</html>