<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<h1>Trader</h1>

<form:form enctype="multipart/form-data">
  
    <input type="submit" name="action" value="Connect" />
    <input type="submit" name="action" value="Disconnect" />
    <input type="submit" name="action" value="Clear" />

    <form:select path="account">
    	<form:options items="${accounts}" />
    </form:select>

	<input type="file" name="file" accept=".csv" />
	<input type="submit" name="action" value="Import" />
	
   	<form:errors path="*" cssClass="error" />
	   
</form:form>

<div class="table">
    <display:table name="actionItems" >
        <display:column property="description" title="Action Items"></display:column>
    </display:table>
</div>

<div class="table">
    <display:table name="messages" >
        <display:column property="date" title="Messages &amp; Errors"></display:column>
        <display:column property="text" title="" maxLength="100"></display:column>
    </display:table>
</div>    