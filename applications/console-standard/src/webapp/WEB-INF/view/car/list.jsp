<%@ page import="org.apache.geronimo.console.util.PortletManager"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>
<portlet:defineObjects/>
<p>These are the configurations available in the selected repository.  The entries
that are hyperlinks may be installed into the local server.  The other entries are
already available in the local server.</p>

<p><b>Available Configurations:</b></p>

<c:forEach var="category" items="${categories}">
  <p>${category.key}</p>
  <ul>
    <c:forEach var="entry" items="${category.value}">
      <c:choose>
        <c:when test="${entry.installed}">
          <li>${entry.description}</li>
        </c:when>
        <c:otherwise>
          <li><a href="<portlet:actionURL><portlet:param name="configId" value="${entry.configId}"/><portlet:param name="repository" value="${repository}"/><portlet:param name="repo-user" value="${repouser}"/><portlet:param name="repo-pass" value="${repopass}"/><portlet:param name="mode" value="download-before"/></portlet:actionURL>">${entry.description}<c:if test="${entry.description ne entry.configId}"> (${entry.version})</c:if></a></li>
        </c:otherwise>
      </c:choose>
    </c:forEach>
  </ul>
</c:forEach>

<p><a href="<portlet:actionURL portletMode="view"><portlet:param name="mode" value="index-before" /></portlet:actionURL>">Cancel</a></p>
