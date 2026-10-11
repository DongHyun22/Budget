<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>요청 안내 · 하루 예산</title>
    <link rel="stylesheet" href="<c:url value='/css/theme.css'/>">
<link rel="stylesheet" href="<c:url value='/css/app.css'/>">
</head>
<body>
<c:set var="publicPage" value="true"/>
<%@ include file="../fragments/header.jspf" %>
<main id="main-content" class="auth-page">
    <section class="auth-card" aria-labelledby="error-heading">
        <p class="eyebrow">하루 예산 · <c:out value="${statusCode}"/></p>
        <h1 id="error-heading">요청을 확인해 주세요</h1>
        <p role="alert"><c:out value="${errorMessage}"/></p>
        <a class="button primary" href="<c:url value='/budget'/>">가계부로 이동</a>
    </section>
</main>
<%@ include file="../fragments/footer.jspf" %>
</body>
</html>
