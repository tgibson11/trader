<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<h1>Trader</h1>

<form action="/trader/home" method="post" enctype="multipart/form-data">
  
    <input type="submit" name="connect" value="Connect" />
    <input type="submit" name="disconnect" value="Disconnect" />
	<input type="submit" name="clear" value="Clear Messages" />

	<select name="account">
		<c:forEach items="${accounts}" var="account">
			<option value="${account.accountId}"><c:out value="${account.accountName}" /></option>
		</c:forEach>
	</select>

	<input type="file" name="file" accept=".csv" />
	<input type="submit" name="import" value="Import Orders" />
	
	<div class="table">
	    <display:table name="actionItems" uid="actionItem">
	        <display:column property="description" title="Action Items"></display:column>
	        <display:column title="Ignore" class="align-center">
	        	<input type="radio" name="actionItems[${actionItem_rowNum - 1}]" value="false"/>
	        </display:column>
	        <display:column title="Execute" class="align-center">
	        	<input type="radio" name="actionItems[${actionItem_rowNum - 1}]" value="true"/>
	        </display:column>
	    </display:table>
	</div>

	<input type="submit" name="submit" value="Submit Action Items" />
	<input type="button" id="clear-action-items" value="Clear Selected Action Items" />

	<div class="table">
	    <display:table name="messages" >
	        <display:column property="date" title="Messages &amp; Errors"></display:column>
	        <display:column property="text" title="" maxLength="100"></display:column>
	    </display:table>
	</div> 
	
</form>


<script type="text/javascript">
	$(document).ready(function() {
		$("#clear-action-items").click(function() {
			$("#actionItem input:radio").prop('checked', false);
		});
	});
</script>  