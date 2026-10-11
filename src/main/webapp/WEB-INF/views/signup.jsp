<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>회원가입 · 하루 예산</title>
    <link rel="stylesheet" href="<c:url value='/css/theme.css'/>">
<link rel="stylesheet" href="<c:url value='/css/app.css'/>">
</head>
<body>
<c:set var="publicPage" value="true"/>
<%@ include file="fragments/header.jspf" %>
<main id="main-content" class="auth-page">
    <section class="auth-card" aria-labelledby="signup-heading">
        <p class="eyebrow">하루 예산</p>
        <h1 id="signup-heading">나의 하루 예산 시작하기</h1>
        <p class="muted">예산과 지출을 기록하고 오늘 쓸 수 있는 금액을 확인하세요.</p>
        <c:if test="${not empty errorMessage}"><p class="alert error" role="alert"><c:out value="${errorMessage}"/></p></c:if>
        <form method="post" action="<c:url value='/signup'/>">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <label for="name">이름</label>
            <input id="name" name="name" maxlength="50" autocomplete="name" value="<c:out value='${name}'/>" required autofocus>
            <label for="email">이메일</label>
            <input id="email" name="email" type="email" maxlength="254" autocomplete="username" value="<c:out value='${email}'/>" required>
            <label for="password">비밀번호</label>
            <input id="password" name="password" type="password" minlength="8" maxlength="64" autocomplete="new-password" aria-describedby="password-help" required>
            <p id="password-help" class="hint">8~64자, 한글 등 다중 바이트 문자를 포함하면 UTF-8 기준 72바이트 이하</p>
            <label for="passwordConfirm">비밀번호 확인</label>
            <input id="passwordConfirm" name="passwordConfirm" type="password" minlength="8" maxlength="64" autocomplete="new-password" required>
            <button type="submit" class="button primary">회원가입</button>
        </form>
        <p>이미 계정이 있으신가요? <a href="<c:url value='/login'/>">로그인</a></p>
    </section>
</main>
<%@ include file="fragments/footer.jspf" %>
</body>
</html>
