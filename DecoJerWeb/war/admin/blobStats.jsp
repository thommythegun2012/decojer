<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<jsp:useBean id="blobStats" class="org.decojer.web.controller.BlobStats" />
<%
	blobStats.calculateStats();
%>

<jsp:include page="/WEB-INF/template/head.jsp" />
<jsp:include page="/WEB-INF/template/body.jsp" />

<h2>Blob Stats:</h2>
<c:if test="${blobStats.stats.calculatedHashes gt 0}">
	<p>Calculated Hashes: ${blobStats.stats.calculatedHashes}</p>
</c:if>
<p>Double Hashes: ${blobStats.stats.doubleHashes}</p>
<p>Number: ${blobStats.stats.number}</p>
<p>Size: ${blobStats.stats.size}</p>

<jsp:include page="/WEB-INF/template/foot.jsp" />