<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
	import="org.decojer.web.util.*"%>
<jsp:useBean id="upload" class="org.decojer.web.controller.Upload" />
<!DOCTYPE HTML>
<html lang="en-us">
<head>
<jsp:include page="/WEB-INF/template/syntaxHl.jsp" />
<jsp:include page="/WEB-INF/template/head.jsp" />
</head>
<head>
<body>
	<jsp:include page="/WEB-INF/template/header.jsp" />
	<p>This is currently only a test page! The Java Decompiler is not
		yet finished. Currenty working on:</p>
	<ul>
		<li>Add Data Flow Analysis: variable Types and Names, local
			declarations</li>
		<li>Complete Control Flow Analysis: break support</li>
		<li>Add Exceptions</li>
		<li>Improve Android/Dalvik result: recognize temporary registers</li>
	</ul>
	<form action="${upload.uploadUrl}" method="post"
		enctype="multipart/form-data">
		<input type="file" name="file"><input type="submit"
			value="Submit">
	</form>
	<%=Messages.getMessagesHtml(pageContext.getSession())%>
	<%=Uploads.getUploadsHtml(request, pageContext.getSession())%>
</body>
</html>