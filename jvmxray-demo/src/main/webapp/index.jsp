<%@ page import="java.text.*,java.util.*,java.io.*" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>JVMXRayDemo</title>
</head>
<body>
<h1><%= "JVMXRay Demo" %>
</h1>
This demo provides a method to demostrate a few insecure web practices that trigger various events within JVMXRay.
<p/>
<a href="/">[REFRESH]</a>
<p/>
<b>File CRUD Operations</b><br/>
<a href="/helloservlet/api/readfile">Read File</a><br/>
<a href="/helloservlet/api/writefile">Write File</a><br/>
<a href="/helloservlet/api/deletefile">Delete File</a><br/>
<p/>
<b>Environment</b><br/>
<a href="/helloservlet/api/shellexecute">Shell Execution</a><br/>
<a href="/helloservlet/api/readproperties">Read Java Properties</a><br/>
<a href="/helloservlet/api/systemstreams">Change System Streams(e.g., System.out/System.err)</a><br/>
<p/>
<b>Network</b><br/>
<a href="/helloservlet/api/sockets">Read Java Socket</a><br/>
<p/>
<b>Message</b><br/>
<%
    Object obj = request.getAttribute("message");
    String msg = "";
    if( obj != null ) {
        msg = obj.toString();
    }
%>
<%=msg%>
<p/>
<a href="/">[REFRESH]</a>
</body>
</html>