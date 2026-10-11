<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>로그인 · 하루 예산</title>
    <link rel="stylesheet" href="<c:url value='/css/theme.css'/>">
<link rel="stylesheet" href="<c:url value='/css/app.css'/>">
</head>
<body>
<c:set var="publicPage" value="true"/>
<%@ include file="fragments/header.jspf" %>
<main id="main-content" class="auth-page">
    <section class="auth-card" aria-labelledby="login-heading">
        <p class="eyebrow">하루 예산</p>
        <h1 id="login-heading">오늘의 여유를 확인하세요</h1>
        <p class="muted">매일 달라지는 예산을 한눈에 관리하세요.</p>
        <c:if test="${param.error != null}"><p class="alert error" role="alert">이메일 또는 비밀번호를 확인해 주세요.</p></c:if>
        <c:if test="${param.registered != null}"><p class="alert success" role="status">가입이 완료되었습니다. 로그인해 주세요.</p></c:if>
        <c:if test="${param.logout != null}"><p class="alert" role="status">로그아웃되었습니다.</p></c:if>
        <form method="post" action="<c:url value='/login'/>">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <label for="email">이메일</label>
            <input id="email" name="email" type="email" maxlength="254" autocomplete="username" required autofocus>
            <label for="password">비밀번호</label>
            <input id="password" name="password" type="password" autocomplete="current-password" required>
            <button type="submit" class="button primary">로그인</button>
        </form>
        <p>처음이신가요? <a href="<c:url value='/signup'/>">회원가입</a></p>
    </section>
</main>
<%@ include file="fragments/footer.jspf" %>
</body>
</html>
