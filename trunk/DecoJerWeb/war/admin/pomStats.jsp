<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<jsp:useBean id="pomStats" class="org.decojer.web.controller.PomStats" />
<%
	pomStats.calculateStats();
%>

<jsp:include page="/WEB-INF/template/head.jsp" />
<jsp:include page="/WEB-INF/template/body.jsp" />

<h2>POM Stats:</h2>
<p>Number: ${pomStats.stats.number}</p>
<p>Size: ${pomStats.stats.size}</p>

<jsp:include page="/WEB-INF/template/foot.jsp" />