<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>일일 알림 | 변동지출 가계부</title><link rel="stylesheet" href="<c:url value='/css/theme.css'/>">
<link rel="stylesheet" href="<c:url value='/css/app.css'/>"></head>
<body>
<c:set var="activePage" value="notifications"/>
<%@ include file="fragments/header.jspf" %>
<main id="main-content" class="shell"><p class="eyebrow">DAILY NOTICE</p><h1>일일 사용 가능 금액 알림</h1><p class="muted">한국시간 기준 하루 시작 배정액입니다. 지출 후 현재 남은 금액은 가계부에서 확인하세요.</p>
<section class="panel"><h2>최근 알림 · 최대 90건</h2>
<c:if test="${empty notifications}"><p class="empty">아직 알림이 없습니다. 월 예산이 설정되어 있으면 다음 일일 작업에서 알림이 생성됩니다.</p></c:if>
<ul class="notification-list"><c:forEach var="notice" items="${notifications}"><li><span class="badge"><c:out value="${notice.noticeDate}"/></span>
<p>하루 시작 사용 가능 금액 <strong><fmt:formatNumber value="${notice.openingAmount}" pattern="#,##0"/>원</strong></p>
<small>생성 시각 <c:out value="${notice.createdAt}"/> · 생성 당시 금액이며 이후 수정으로 달라질 수 있습니다.</small>
<c:choose><c:when test="${empty notice.readAt}"><form method="post" action="<c:url value='/notifications/${notice.id}/read'/>"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="btn-secondary">읽음 처리</button></form></c:when><c:otherwise><span class="badge">읽음</span></c:otherwise></c:choose>
</li></c:forEach></ul></section></main><%@ include file="fragments/footer.jspf" %>
</body></html>
