<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<jsp:useBean id="maven" class="org.decojer.web.controller.Maven" />
<%
	maven.importCentralAll();
%>

<jsp:include page="/WEB-INF/template/head.jsp" />
<jsp:include page="/WEB-INF/template/body.jsp" />

<h2>Check POMs and Import from Maven Central RSS</h2>
<p>Imported: ${maven.importResults.imported}</p>

<jsp:include page="/WEB-INF/template/foot.jsp" />