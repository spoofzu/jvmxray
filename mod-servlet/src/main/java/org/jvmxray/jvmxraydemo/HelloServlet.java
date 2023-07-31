package org.jvmxray.jvmxraydemo;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet(name = "helloservlet", value = "/helloservlet/api/*")
public class HelloServlet extends HttpServlet {

    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        String pathinfo = request.getPathInfo();
        System.out.println("servlet pathinfo="+pathinfo);
        String msg = "";
        switch(pathinfo) {
            case "/readfile":
                msg = readFile();
                break;
            case "/writefile":
                msg = writeFile();
                break;
            case "/deletefile":
                msg = deleteFile();
                break;
            case "/shellexecute":
                msg = shellExecute();
                break;
            case "/readproperties":
                msg = readProperties();
                break;
            case "/systemstreams":
                msg = systemStreams();
                break;
            case "/sockets":
                msg = sockets();
                break;
        }
        request.setAttribute("message",msg);
        response.setStatus(200);
        request.getRequestDispatcher("/").forward(request, response);
    }

    // Open socket connection to this server (and do nothing).
    private String sockets() {
        String msg = "";
        try {
            URL url = new URL("http://localhost:8080/");
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            in.close();
            msg = "Opened socket to, "+url;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    // Reassign System.out temporarily and then restore.
    private String systemStreams() {
        String msg = "";
        PrintStream out = System.out;
        PrintStream nout = new PrintStream(new ByteArrayOutputStream());
        System.setOut(nout);
        System.setOut(out);
        msg = "Replaced System.out with "+nout.toString()+" and restored.";
        return msg;
    }

    // Creates some sample properties, saves them, then reads them.
    private String readProperties() {
        String msg = "";
        try {
            // Create a sample properties file with some data.
            File file = File.createTempFile("jvmxraytestpr-", ".tmp");
            file.deleteOnExit();
            Properties p = new Properties();
            p.setProperty("settingA","this is a test property");
            PrintWriter pw = new PrintWriter(file);
            p.store(pw,"sample xray props for testing ok to delete");
            // Read it back.
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = "";
            while((line = br.readLine()) != null) {
                System.out.println(line);
            }
            msg = "Created tmp file, "+file+" with sample data and read output.";
        } catch(Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    // Executes innocuous shell command.
    private String shellExecute() {
        String msg = "";
        try {
            String cmd = "cd .";
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(cmd);
            long initialtime = System.currentTimeMillis();
            long elapsed = 0;
            boolean isAlive = p.isAlive();
            while( isAlive && elapsed<2500) {
                Thread.currentThread().yield();
                Thread.currentThread().sleep(100);
                elapsed = System.currentTimeMillis() - initialtime;
                isAlive = p.isAlive();
            }
            if(isAlive) {
                // An unlikely case.
                msg = "Following shell command timedout (response>2.5sec), cmd="+cmd;
            } else {
                msg = "Following shell command executed, cmd='" + cmd + "' result code=" + p.exitValue();
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    // Create a file then delete it.
    private String deleteFile() {
        String msg = "";
        try {
            // Create a sample file and delete it.
            File file = File.createTempFile("jvmxraytestd-", ".tmp");
            file.delete();
            msg = "Following file was created then deleted. file="+file;
        } catch(Exception e){
            e.printStackTrace();
        }
        return msg;
    }

    // Create a file.
    private String writeFile() {
        String msg = "";
        try {
            File file = File.createTempFile("jvmxraytestw-", ".tmp");
            file.deleteOnExit();
            PrintWriter pw = new PrintWriter(file);
            pw.println("All fun and no play makes Jack a dull boy.");
            pw.close();
            msg = "The following file was created and written with sample data. file="+file;
        } catch(Exception e){
            e.printStackTrace();
        }
        return msg;
    }

    // Create a file with sample data then read it.
    private String readFile() {
        String msg = "";
        try {
            // Create a sample.
            File file = File.createTempFile("jvmxraytestr-", ".tmp");
            file.deleteOnExit();
            PrintWriter pw = new PrintWriter(file);
            pw.println("All fun and no play makes Jack a dull boy.");
            pw.close();
            // Read it back.
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = "";
            while((line = br.readLine()) != null) {
                System.out.println(line);
            }
            msg = "The following file was created with sample data and then read. file="+file;
        } catch(Exception e){
            e.printStackTrace();
        }
        return msg;
    }

    public void destroy() {
    }
}