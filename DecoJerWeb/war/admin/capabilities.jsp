<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<jsp:useBean id="capabilities"
	class="org.decojer.web.controller.Capabilities" />

<jsp:include page="/WEB-INF/template/head.jsp" />
<jsp:include page="/WEB-INF/template/body.jsp" />

<h2>Capabilities:</h2>
<table>
	<tr>
		<th>Capabilitiy</th>
		<th>State</th>
		<th>Maintenance</th>
	</tr>
	<c:forEach items="${capabilities.capabilities}" var="_c">
		<tr>
			<td>${_c.capability.packageName}.${_c.capability.name}</td>
			<td>${_c.status}</td>
			<td>${_c.scheduledDate}</td>
		</tr>
	</c:forEach>
</table>

<jsp:include page="/WEB-INF/template/foot.jsp" />