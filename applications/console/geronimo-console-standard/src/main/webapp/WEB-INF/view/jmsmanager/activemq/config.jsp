<%--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>
<portlet:defineObjects/>
<script language="javascript">
<!--
    function doSave(){
        document.datasource_form.action="<portlet:actionURL portletMode="view"/>";
        document.datasource_form.mode.value="save";
        return true;
    }
    function doCancel(){
        document.datasource_form.action="<portlet:actionURL portletMode="view"/>";
        document.datasource_form.mode.value="detail";
        return true;
    }
//-->
</script>
    
<form name="datasource_form">
<input type="hidden" name="name" value="${ds.objectName}" />
<input type="hidden" name="mode" value="detail" />

<br>
<strong>Connection Name:</strong>&nbsp;${ds.name}
<br><br>
<table width="100%">
    <tr><td><strong>UserName</strong></td><td><input type="text" name="UserName" value="${ds.userName}" size="75" /></td></tr>
    <tr><td><strong>Password</strong></td><td><input type="password" name="password1" size="75" /></td></tr>
    <tr><td><strong>Repeat&nbsp;Password</strong></td><td><input type="password" name="password2" size="75" /></td></tr>
<c:if test="${badPassword}"><tr><td colspan="2">Passwords did not match</td></tr></c:if>
    <tr><td><strong>ServerUrl</strong></td><td><input type="text" name="ServerUrl" value="${ds.serverUrl}" size="75" /></td></tr>
    <!--<tr><td><strong>Clientid</strong></td><td><input type="text" name="Clientid" value="${ds.clientid}" size="75" /></td></tr>


    <tr><td><strong>Partition Max Size</strong></td><td><input type="text" name="partitionMaxSize" value="${connectionManagerInfo.partitionMaxSize}" size="75" /></td></tr>
    <tr><td><strong>Partition Min Size</strong></td><td><input type="text" name="partitionMinSize" value="${connectionManagerInfo.partitionMinSize}" size="75" /></td></tr>
    <tr><td><strong>Blocking Timeout (Milliseconds)</strong></td><td><input type="text" name="blockingTimeoutMilliseconds" value="${connectionManagerInfo.blockingTimeoutMilliseconds}" size="75" /></td></tr>
    <tr><td><strong>Idle Timeout (Minutes)</strong></td><td><input type="text" name="idleTimeoutMinutes" value="${connectionManagerInfo.idleTimeoutMinutes}" size="75" /></td></tr>-->
    <tr><td colspan="2"><input type="submit" name="btnSave" value="Save" onClick="doSave();"/><input type="submit" name="btnCancel" value="Cancel" onClick="doCancel();"></td></tr>
</table>
</form>