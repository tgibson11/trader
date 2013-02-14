<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<h1>Rollover Contracts</h1>

<form:form cssClass="verticalForm">
    <div>
        <label for="symbol">Symbol:</label>
        <form:select path="symbol" items="${contracts}" itemLabel="symbol" itemValue="symbol" />
        <form:errors path="symbol" cssClass="error" />
    </div>
    <div>
        <label for="expiry">Expiry:</label>
        <form:select path="expiry" items="${expiries}" />
        <form:errors path="expiry" cssClass="error" />
    </div>
    <div>
        <input type="submit" value="Submit" />
    </div>
</form:form>

<div class="table">
    <display:table name="rollovers" uid="rollover">
        <display:column title="Symbol" property="currentContract.symbol" />
        <display:column title="Current Expiry"  property="currentContract.expiry" />
        <display:column title="Next Expiry"  property="nextContract.expiry" />
        <display:column title="Current Volume" property="currentContract.volume"/>
        <display:column title="Next Volume" property="nextContract.volume"/>
        <display:column title="Open Position">
            <c:choose>
                <c:when test="${rollover.currentContract.openPosition}">
                    <input type="checkbox" disabled="disabled" checked="checked" />
                </c:when>
                <c:otherwise>
                    <input type="checkbox" disabled="disabled" />
                </c:otherwise>
            </c:choose>
        </display:column>
        <display:column>
            <a href="${rollover.currentContract.openInterestUrl}">
                <c:out value="${rollover.currentContract.symbol}" /> Open Interest
            </a>
        </display:column>
    </display:table>
</div>