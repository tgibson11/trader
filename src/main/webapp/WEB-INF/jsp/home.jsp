<%@ include file="/WEB-INF/jsp/common/taglibs.jsp" %>

<h1>Trader</h1>

<form:form>
  
    <input type="submit" name="_connectTWS" value="Connect TWS" />
    <input type="submit" name="_disconnectTWS" value="Disconnect TWS" />
    <input type="submit" name="_checkRollovers" value="Check Rollovers" />
    <input type="submit" name="_positionSizing" value="Position Sizing" />
   
</form:form>

<div class="table">
    <display:table name="info" >
        <display:column property="date" title="Messages &amp; Errors"></display:column>
        <display:column property="text" title="" maxLength="100"></display:column>
    </display:table>
</div>    