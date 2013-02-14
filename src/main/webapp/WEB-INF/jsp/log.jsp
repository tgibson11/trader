<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>
    
<h1>TWS Data Log</h1>

<div class="table">
	<display:table name="data">
	    <display:column property="date" title=""></display:column>
	    <display:column property="text" title="" maxLength="100"></display:column>
	</display:table>
</div>