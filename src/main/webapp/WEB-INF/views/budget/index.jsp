<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html lang="ko"><head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="csrf-token" content="<c:out value='${_csrf.token}'/>">
  <meta name="csrf-header" content="<c:out value='${_csrf.headerName}'/>">
  <title>월별 가계부 · 하루예산</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/theme.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
  <script type="module" src="${pageContext.request.contextPath}/js/ledger/app.mjs"></script>
</head><body>
<c:set var="activePage" value="budget"/>
<%@ include file="../fragments/header.jspf" %>
<main id="main-content" class="shell ledger-page" data-context="${pageContext.request.contextPath}" data-month="${summary.ym}" data-today="${today}">
  <div class="page-heading">
    <div><p class="eyebrow">YOUR DAILY LEDGER</p><h1>월별 가계부</h1><p class="muted">하루의 기록을 모아, 한 달의 흐름을 봅니다.</p></div>
    <form id="month-form" method="get" action="${pageContext.request.contextPath}/budget">
      <label for="month">조회 월</label><input id="month" name="ym" type="month" min="1000-01" max="9999-12" value="${summary.ym}" required><button class="btn" type="submit">조회</button>
    </form>
  </div>
  <noscript><p class="alert">날짜 상세와 지출 입력에는 JavaScript가 필요합니다. JavaScript를 켠 뒤 다시 열어 주세요. 월별 표는 계속 조회할 수 있습니다.</p></noscript>
  <p id="page-status" class="status-message" role="status" aria-live="polite"></p>
  <div id="page-recovery" hidden><button id="retry-summary" class="btn" type="button">조회 다시 시도</button> <a href="${pageContext.request.contextPath}/login">다시 로그인</a></div>
  <p id="no-plan" class="alert" ${empty summary.planAmount ? '' : 'hidden'}>아직 월 예산을 설정하지 않았습니다. 예산 없이도 지출을 기록할 수 있어요.</p>
  <section class="metrics" aria-label="월별 요약">
    <div class="metric"><span>월 예산</span><strong id="metric-plan"><c:choose><c:when test="${empty summary.planAmount}">미설정</c:when><c:otherwise><fmt:formatNumber value="${summary.planAmount}"/>원</c:otherwise></c:choose></strong></div>
    <div class="metric"><span>이번 달 지출</span><strong id="metric-spent"><fmt:formatNumber value="${summary.totalSpent}"/>원</strong></div>
    <div class="metric"><span>월 예산 잔액</span><strong id="metric-remaining" class="${not empty summary.planAmount and summary.remaining lt 0 ? 'negative' : ''}"><c:choose><c:when test="${empty summary.planAmount}">미설정</c:when><c:otherwise><fmt:formatNumber value="${summary.remaining}"/>원</c:otherwise></c:choose></strong></div>
    <div class="metric"><span>하루 기준액</span><strong id="metric-base"><c:choose><c:when test="${empty summary.planAmount}">미설정</c:when><c:otherwise><fmt:formatNumber value="${summary.baseAmount}"/>원</c:otherwise></c:choose></strong></div>
  </section>
  <div class="notice-strip"><p id="today-summary"><c:if test="${not empty todayAllowance and not empty summary.planAmount}">오늘 ${today} · 현재 남은 금액 <fmt:formatNumber value="${todayAllowance.remaining}"/>원</c:if></p><c:if test="${not empty dailyNotice}"><p class="muted">오늘 알림 · <c:out value="${dailyNotice.noticeDate}"/> 생성 당시 시작 금액 <fmt:formatNumber value="${dailyNotice.openingAmount}"/>원. 이후 기록은 현재 금액에 반영됩니다. <a href="${pageContext.request.contextPath}/notifications">알림 확인</a></p></c:if></div>
  <section class="panel" aria-labelledby="ledger-heading">
    <div class="selection-toolbar"><div><p class="eyebrow">DAILY FLOW</p><h2 id="ledger-heading">일별 예산 흐름</h2></div><button id="open-plan" class="btn" type="button" disabled>월 예산 설정</button></div>
    <p id="ledger-help" class="muted">날짜를 선택한 뒤 상세를 열어 보세요. 더블클릭 또는 Enter로도 열 수 있습니다. 남은 금액은 다음 날로 이월되며, 다음 달에는 새로 시작합니다.</p>
    <div class="table-wrap"><table class="ledger-table" aria-describedby="ledger-help"><thead><tr><th scope="col">날짜</th><th scope="col">하루 기준액</th><th scope="col">시작 배정액</th><th scope="col">지출</th><th scope="col">남은 금액 · 이월</th></tr></thead><tbody id="ledger-days">
      <c:forEach items="${summary.days}" var="day"><tr data-date="${day.date}" class="${day.date eq defaultDate ? 'is-selected' : ''}">
        <th scope="row"><label class="ledger-date"><input type="radio" name="ledger-date" value="${day.date}" ${day.date eq defaultDate ? 'checked' : ''}><span>${day.date}<c:if test="${day.date eq today}"> · 오늘</c:if></span></label></th>
        <c:choose><c:when test="${empty summary.planAmount}"><td>미설정</td><td>미설정</td><td class="number"><fmt:formatNumber value="${day.spent}"/>원</td><td>미설정</td></c:when><c:otherwise><td class="number"><fmt:formatNumber value="${day.base}"/>원</td><td class="number ${day.allowed lt 0 ? 'negative' : ''}"><fmt:formatNumber value="${day.allowed}"/>원</td><td class="number"><fmt:formatNumber value="${day.spent}"/>원</td><td class="number ${day.remaining lt 0 ? 'negative' : ''}"><fmt:formatNumber value="${day.remaining}"/>원</td></c:otherwise></c:choose>
      </tr></c:forEach>
    </tbody></table></div>
    <p id="unallocated" class="muted">월말 미배정 잔액 <fmt:formatNumber value="${summary.unallocated}"/>원 · 일별 예산에 더하지 않습니다.</p>
  </section>
  <nav class="ledger-dock" aria-label="선택일 작업">
    <p id="selected-date" aria-live="polite">${defaultDate} 선택됨</p>
    <div class="dialog-actions"><button id="open-detail" class="btn secondary" type="button" disabled>선택일 상세</button><button id="open-spend" class="btn primary" type="button" disabled>+ 지출 입력</button></div>
  </nav>
  <%@ include file="dialogs.jspf" %>
</main>
<%@ include file="../fragments/footer.jspf" %>
</body></html>
