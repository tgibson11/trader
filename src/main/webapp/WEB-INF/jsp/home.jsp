<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<h1>Trader</h1>

<form:form enctype="multipart/form-data">
  
    <input type="submit" name="action" value="Connect" />
    <input type="submit" name="action" value="Disconnect" />

    <form:select path="account">
    	<form:options items="${accounts}" />
    </form:select>

	<input type="file" name="file" accept=".csv" />
	<input type="submit" name="action" value="Import Orders" />
	
   	<form:errors path="*" cssClass="error" />
	   
	<div class="table">
	    <display:table name="actionItems" uid="actionItem">
	        <display:column property="description" title="Action Items"></display:column>
	        <display:column title="Ignore" class="align-center">
	        	<form:radiobutton path="submittedActionItems[${actionItem_rowNum - 1}]" value="false" />
	        </display:column>
	        <display:column title="Execute" class="align-center">
	        	<form:radiobutton path="submittedActionItems[${actionItem_rowNum - 1}]" value="true" />
	        </display:column>
	    </display:table>
	</div>
	<div>
		<input type="submit" name="action" value="Submit Action Items" />
		<input type="button" id="clear-action-items" value="Clear Selected Action Items" />
	</div>
	<div>
		<input type="submit" name="action" value="Clear Messages" />
	</div>
	<div class="table">
	    <display:table name="messages" >
	        <display:column property="date" title="Messages &amp; Errors"></display:column>
	        <display:column property="text" title="" maxLength="100"></display:column>
	    </display:table>
	</div> 
	
</form:form>


<script type="text/javascript">
	$(document).ready(function() {
		$("#clear-action-items").click(function() {
			$("#actionItem input:radio").prop('checked', false);
		});
	});
</script>  