<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<jsp:useBean id="urlFetch" class="org.decojer.web.controller.UrlFetch" />
<jsp:setProperty name="urlFetch" property="*" />

<jsp:include page="/WEB-INF/template/head.jsp" />
<jsp:include page="/WEB-INF/template/body.jsp" />

<p>URL Fetch:</p>
<form>
	<input type="text" name="url" value="${urlFetch.url}"><input
		type="submit" value="Submit">
</form>
<p>
	<c:out value="${urlFetch.fetchResult}" escapeXml="true" />
</p>

<jsp:include page="/WEB-INF/template/foot.jsp" />