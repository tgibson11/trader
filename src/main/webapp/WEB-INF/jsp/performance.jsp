<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<h1>Performance</h1>

<form id="performanceForm" action="/trader/performance" method="post">
    <div>
        <label for="accountId">Account:</label>
        <select id="accountId" name="accountId" onchange="document.getElementById('performanceForm').submit();">
        	<c:forEach items="${accounts}" var="account">
        		<c:choose>
        			<c:when test="${ account.accountId == selectedAccountId }">
        				<option value="${account.accountId}" selected="selected"><c:out value="${account.accountName}" /></option>
        			</c:when>
        			<c:otherwise>
        				<option value="${account.accountId}"><c:out value="${account.accountName}" /></option>
        			</c:otherwise>
        		</c:choose>
        	</c:forEach>
        </select>
    </div>
    <div>
        <fieldset>
            <legend>Update</legend>
	        <label for="date">Date:</label>
	        <select id="date" name="date">
	        	<c:forEach items="${performanceData}" var="data">
	        		<c:set var="date"><fmt:formatDate value="${data.date}" pattern="MM/dd/yyyy" /></c:set>
	        		<option value="${date}">${date}</option>
	        	</c:forEach>
	        </select>
	        <label for="deposits">Deposits:</label>
	        <input type="text" id="deposits" name="deposits" />
	        <label for="withdrawals">Withdrawals:</label>
	        <input type="text" id="withdrawals" name="withdrawals" />
	        <label for="nav">NAV:</label>
	        <input type="text" id="nav" name="nav" />
	        <input type="submit" name="_update" value="Submit" />
        </fieldset>
    </div>
</form>

<div class="table">
    <display:table id="performanceTable" name="performanceData" requestURI="performance" sort="list" defaultsort="1" defaultorder="descending">
     <display:column property="date" title="Month" decorator="trader.support.display.tag.MonthYearColumnDecorator" sortable="true" class="align-left" />
     <display:column property="bnav" title="BNAV" decorator="trader.support.display.tag.CurrencyColumnDecorator" sortable="true" />
     <display:column property="deposits" title="Additions" decorator="trader.support.display.tag.CurrencyColumnDecorator" sortable="true" />
     <display:column property="withdrawals" title="Withdrawals" decorator="trader.support.display.tag.CurrencyColumnDecorator" sortable="true" />
     <display:column property="enav" title="ENAV" decorator="trader.support.display.tag.CurrencyColumnDecorator" sortable="true" />
     <display:column property="performance" title="Net Performance" decorator="trader.support.display.tag.CurrencyColumnDecorator" sortable="true" />
     <display:column property="ror" title="ROR" decorator="trader.support.display.tag.PercentColumnDecorator" sortable="true" />
     <display:column property="vami" title="VAMI" decorator="trader.support.display.tag.CurrencyColumnDecorator" sortable="true" />
     <display:column property="peakVami" title="Peak VAMI" decorator="trader.support.display.tag.CurrencyColumnDecorator" sortable="true" />
     <display:column property="drawdown" title="Drawdown" decorator="trader.support.display.tag.PercentColumnDecorator" sortable="true" />
     <display:column property="cagr" title="CAGR" decorator="trader.support.display.tag.PercentColumnDecorator" sortable="true" />
     <display:column property="expectedAnnualPerformance" title="Expected Annual Performance" decorator="trader.support.display.tag.CurrencyColumnDecorator" sortable="true" />
    </display:table>
</div>

<h2>Summary</h2>

<div class="table">
    <table id="performanceSummary">
        <thead>
            <tr>
                <th></th>
                <c:forEach items="${performanceSummary}" var="year">
                    <th><c:out value="${year.year}" /></th>
                </c:forEach>
            </tr>
        </thead>
        <tbody>
            <tr>
                <th>January</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.janRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>February</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.febRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>March</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.marRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>April</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.aprRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>May</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.mayRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>June</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.junRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>July</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.julRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>August</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.augRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>September</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.sepRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>October</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.octRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>November</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.novRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
            <tr>
                <th>December</th>
                <c:forEach items="${performanceSummary}" var="year">
                    <td><fmt:formatNumber value="${year.decRor}" type="percent" minFractionDigits="2" /></td>
                </c:forEach>
            </tr>
        </tbody>
        <tfoot>
            <tr>
                <th></th>
                <c:forEach items="${performanceSummary}" var="year">
                    <th><fmt:formatNumber value="${year.yearRor}" type="percent" minFractionDigits="2" /></th>
                </c:forEach>
            </tr>
        </tfoot>
    </table>
</div>

<h2>Growth of $1000</h2>

<!-- chart will go here -->
<div id="vamiChartDiv"></div>

<script type="text/javascript">

    // Require the basic 2d chart resource: Chart2D
    dojo.require("dojox.charting.Chart2D");
        
    // Require the theme "Claro"
    dojo.require("dojox.charting.themes.Claro");

    // When the DOM is ready and resources are loaded...
    dojo.ready(function() {

        // Create the chart within it's "holding" node
        var vamiChart = new dojox.charting.Chart2D("vamiChartDiv");
        var vamiChartData = ${vamiChartData};
         
        // Set the theme
        vamiChart.setTheme(dojox.charting.themes.Claro);
 
        // Add the only/default plot 
        vamiChart.addPlot("default", { type: "Columns" });
 
        // Add axes
        vamiChart.addAxis("x",{ labelFunc: labelfMonth, majorTickStep: 12, minorLabels: false });
        vamiChart.addAxis("y", { min: 0, vertical: true, fixLower: "major", fixUpper: "major" });

        // Add the series of data
        vamiChart.addSeries("VAMI",vamiChartData);

        // Render the chart!
        vamiChart.render();
 
    });
    
    function labelfMonth(colnum) {
        var year = 2008;
        var month = 8;
        var monthsToAdd = parseInt(colnum);
        year = year + Math.floor((month + monthsToAdd) / 12);
        month = (month + monthsToAdd) % 12;
        var d = new Date(year, month - 1, 1, 0, 0, 0, 0);
        return (d.getMonth() + 1) + "/" + d.getFullYear();
    }
</script>