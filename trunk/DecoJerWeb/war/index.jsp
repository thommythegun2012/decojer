<jsp:directive.page contentType="text/html; charset=UTF-8"
	language="java" pageEncoding="UTF-8" />
<!DOCTYPE HTML>
<html lang="en">
<head>
<jsp:include page="WEB-INF/template/head.jsp" />
<%@ page import="com.google.appengine.api.blobstore.*"%>
<%@ page import="org.decojer.web.util.*"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%!BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();%>
</head>
<body>
	<jsp:include page="WEB-INF/template/header.jsp" />
	<h1>DecoJer - A Java Decompiler</h1>
	<p>This is currently only a test page! The Java Decompiler is not
		yet finished. Currenty working on:</p>
	<ul>
		<li>Add Data Flow Analysis: variable Types and Names, local
			declarations</li>
		<li>Complete Control Flow Analysis: break support</li>
		<li>Add Exceptions</li>
		<li>Improve Android/Dalvik result: recognize temporary registers</li>
	</ul>
	<form action="<%=blobstoreService.createUploadUrl("/upload")%>"
		method="post" enctype="multipart/form-data">
		<input type="file" name="file"><input type="submit"
			value="Submit">
	</form>
	<%=Messages.getMessagesHtml(pageContext.getSession())%>
	<%=Uploads.getUploadsHtml(request, pageContext.getSession())%>
</body>
</html>