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
	    <display:table htmlId="action-items-table" name="actionItems" uid="order">
	    	<display:column title="Action" >
	    		<c:choose>
	    			<c:when test="${ order.orderId == 0 }">Place Order</c:when>
	    			<c:otherwise>Cancel Order</c:otherwise>
	    		</c:choose>
	    	</display:column>
	    	<display:column title="Order Action" property="action" />
	    	<display:column title="Quantity" property="totalQuantity" class="align-right" />
	    	<display:column title="Symbol" property="contractSymbol" />
	    	<display:column title="Expiry" property="contractExpiry" />
	    	<display:column title="Price" class="align-right">
	    		<c:choose>
	    			<c:when test="${ order.orderType == 'MKT' }">MKT</c:when>
	    			<c:otherwise><c:out value="${order.auxPrice}" /></c:otherwise>
	    		</c:choose>
	    	</display:column>
	        <display:column title="Ignore" class="align-center">
	        	<label for="ignore-action-item-${order_rowNum - 1}">
	        		<input type="radio" id="ignore-action-item-${order_rowNum - 1}" name="actionItems[${order_rowNum - 1}]" value="false" />
	        	</label>
	        </display:column>
	        <display:column title="Execute" class="align-center">
	        	<label for="execute-action-item-${order_rowNum - 1}">
	        		<input type="radio" id="execute-action-item-${order_rowNum - 1}" name="actionItems[${order_rowNum - 1}]" value="true" />
	        	</label>
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
	$(function() {
		
		$("#action-items-table").DataTable({
			"columnDefs": [ {
				"targets": "_all",
				"orderable": false
			} ],
			"info": false,
			"lengthchange": false,
			"order": [ [ 3, "asc" ], [ 4, "asc" ], [ 1, "asc" ], [ 0, "asc" ] ],
			"paging": false,
			"searching": false
		});
		
		$("#clear-action-items").click(function() {
			$("#action-items-table input:radio").prop('checked', false);
		});
		
	});
</script>  