<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html lang="ko"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>알림 · 하루예산</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/theme.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head><body>
<c:set var="activePage" value="notifications"/>
<%@ include file="fragments/header.jspf" %>
<main id="main-content" class="shell">
    <div class="page-heading"><div><p class="eyebrow">NOTIFICATIONS</p><h1>알림</h1>
        <p class="muted">매일 오전 7시에 오늘 남은 금액을 알려드려요. 알림 생성 당시의 금액이며, 이후 지출은 반영되지 않습니다.</p>
    </div></div>
    <c:if test="${empty notices.content}"><section class="panel"><p class="empty-state">아직 도착한 알림이 없습니다.</p></section></c:if>
    <c:forEach items="${notices.content}" var="notice">
        <section class="panel ${not empty notice.remainingAmount and notice.remainingAmount lt 0 ? 'negative' : ''}">
            <h2><c:out value="${notice.noticeDate}"/> 오늘의 예산</h2>
            <c:choose>
                <c:when test="${empty notice.remainingAmount}">
                    <p>이번 달 예산을 설정하면 오늘 사용 가능한 금액을 알려드릴게요.</p>
                    <a class="btn" href="<c:url value='/budget'/>">예산 설정하기</a>
                </c:when>
                <c:otherwise><p>오늘 남은 금액 <strong><fmt:formatNumber value="${notice.remainingAmount}" pattern="#,##0"/>원</strong></p></c:otherwise>
            </c:choose>
            <p class="muted">생성 시각: <c:out value="${notice.createdAt}"/></p>
            <c:choose>
                <c:when test="${empty notice.readAt}">
                    <form method="post" action="${pageContext.request.contextPath}/notifications/${notice.id}/read">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button class="btn" type="submit">읽음으로 표시</button>
                    </form>
                </c:when>
                <c:otherwise><span class="badge">읽음</span></c:otherwise>
            </c:choose>
        </section>
    </c:forEach>
    <nav aria-label="알림 페이지">
        <c:if test="${notices.hasPrevious()}"><a class="btn" href="?page=${notices.number - 1}">이전</a></c:if>
        <c:if test="${notices.hasNext()}"><a class="btn" href="?page=${notices.number + 1}">다음</a></c:if>
    </nav>
</main>
<%@ include file="fragments/footer.jspf" %>
</body></html>
