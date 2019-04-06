/*--------------------------------------------------------

1. Joshua Glepko / Date: 10-7-18

2. Java version used, if not the official version for the class:

e.g. build 1.8.0_181

3. Precise command-line compilation examples / instructions:

e.g.:

> javac MyWebServer.java


4. Precise examples / instructions to run this program:

e.g.:

> java MyWebServer

5. List of files needed for running the program.

e.g.:

 When being run from the command line ALL FILES/FOLDERS
 MUST BE IN THE SRC FOLDER directory.

 After starting program must go to http://localhost:2540
 in Firefox browser.

 Addnums and directory traversing not currently working.

 a. cat.html
 b. dog.txt
 c. MyWebServer.java
 d. addnums.html

5. Notes:

e.g.:

Sometimes when clicking through directories from the root directory
to the sub-a, then to sub-b, then to cat.html the subdirectory sub-a
will be duplicated in the path. To overcome this removed the duplicate
by concatenating the string, thus other test cases I didn't foresee
may fail.
----------------------------------------------------------*/

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// Worker thread created to do bulk of processing on string requests
// received from client
class WebServerWorker extends Thread {    // using Thread API
    Socket sock;                   //
    WebServerWorker (Socket s) {sock = s;} // Ctor to initialize sock member var
    // function for running socket processing work
    public void run(){
        //
        PrintStream out = null;
        BufferedReader in = null;
        try
        {   // Accepts input from other end of socket
            in = new BufferedReader
                    (new InputStreamReader(sock.getInputStream()));
            // Writes output to other side of socket
            out = new PrintStream(sock.getOutputStream());

            String sockdata;
            // Variable for storing path after duplicate removed.
            String pathCleaned = "dummy";

            // Read in data
            sockdata = in.readLine ();
            System.out.println("request is:" + sockdata);

            // split request on space
            String tmpArray[] = sockdata.split(" ");
            //String reqType = tmpArray[0];
            String pathListing = tmpArray[1];

            // Hack so that we check for duplicate subdir and removes it.
            if (sockdata.length() >= 26)
            {
                if (pathListing.substring(1,5)
                        .equals(pathListing.substring(7, 11)))
                {
                    pathCleaned = pathListing.substring(6);
                }
            }

            String fileType = "dummy";

            // if block checks for request containing fake-cgi from addnums form
            // that is sent from process.
            // sockdata is:GET /cgi/addnums.fake-cgi?person=YourName&num1=4&num2=5 HTTP/1.1
            if (pathListing.contains("fake-cgi"))
            {

                // String parsing in case of question mark
                String tmpPath[] = pathListing.split("\\?");
                String addNumPath = tmpPath[0];
                String addNumHtml = tmpPath[1];
                String htmlArray[] = addNumHtml.split("&");
                String personSection = htmlArray[0];
                String personArray[] = personSection.split("=");
                String personVariable = personArray[1];

                // Store variables for processing
                String num1Variable = htmlArray[1];
                String num2Variable = htmlArray[2];

                // Store variables after separating value from title name
                String value1Array[] = num1Variable.split("=");
                int val1 = Integer.valueOf(value1Array[1]);
                String value2Array[] = num2Variable.split("=");
                int val2 = Integer.valueOf(value2Array[1]);
                int answerToReturn = val1 + val2;

                // Send header response to client preceding html addition result
                out.println("HTTP/1.1 200 OK\r\n" +
                        "<!DOCTYPE html><html>" +
                        "Content-Length: " + 50 + "\r\n" +
                        "Content-Type: " + fileType +
                        "\r\n");

                // Send html comprised of sentence result letting user know.
                out.println("<html>");
                out.println("<head><TITLE> CSC435 Sample Form for AddNum </TITLE></head>");
                out.println("<BODY>");
                out.println("<H1> Addnum Result</H1>");
                out.println("Hi " + personVariable + ", " + val1 + " and " + val2 + " are equal to "
                            + answerToReturn + " .");
                out.println("</FORM> </BODY></html>");

            }
            // Cascade down. If request ends with txt process string
            else if (pathListing.endsWith("txt") || pathListing.endsWith("java"))
            {
                // Send header response preceding text file conversion
                out.println("HTTP/1.1 200 OK\r\n" +
                            "<!DOCTYPE html><html>" +
                            "Content-Length: " + 50 + "\r\n" +
                            "Content-Type: " + fileType +
                            "\r\n");

                // convert file data to bytes and send via socket
                File f1 = new File ( "." + pathListing);
                // retrieve file data as bytes
                InputStream isfile = new FileInputStream(f1);
                // Convert to bytes being written out
                // to client side
                convertPush(isfile, out);

            } // If request ends with html process here
            else if (pathListing.endsWith("html") || pathCleaned.endsWith("html"))
            {
                // Send header response first
                out.println("HTTP/1.1 200 OK\r\n" +
                        "<!DOCTYPE html><html>" +
                        "Content-Length: " + 50 + "\r\n" +
                        "Content-Type: " + fileType +
                        "\r\n");

                // If path had a duplicate and needed to be modified
                // this variable will be changed from dummy.
                if (pathCleaned.equals("dummy"))
                {
                    File f1 = new File ( "." + pathListing);
                    // retrieve file data as bytes
                    InputStream isfile1 = new FileInputStream(f1);
                    // Convert to bytes being written out
                    // to client side
                    convertPush(isfile1, out);
                }
                else if (pathCleaned != "dummy")
                {   // New file object created
                    File f2 = new File ( "." + pathCleaned);
                    // retrieve file data as bytes
                    InputStream isfile2 = new FileInputStream(f2);
                    // Convert to bytes being written out
                    // to client side
                    convertPush(isfile2, out);
                }
            }
            else // Request to see main directory or subdirectory
            {
                // Send header response first
                out.println("HTTP/1.1 200 OK\r\n" +
                "<!DOCTYPE html><html>" +
                "Content-Length: " + 50 + "\r\n" +
                "Content-Type: " + fileType +
                "\r\n");

                // Process path info retrieving bytes
                File tmpFile = new File ( "." + pathListing );
                // Get all the files and directories
                File[] grabDirs = tmpFile.listFiles ( );
                //showFiles(directoryRoot, grabDirs, out);

                // I obtained the design of this function courtesy the ReadFiles.java
                // in the assignment write-up. This function traverses the local
                // computers file path directory to display the held data to the
                // console and broswer/client.
                for ( int i = 0 ; i < grabDirs.length ; i ++ )
                {
                    if ( grabDirs[i].isDirectory ( ) )
                    {   // write directory path string out to client
                        out.println( "<p><a href=" + grabDirs[i].getPath() + ">" +
                                grabDirs[i].getName() + "</a></P>");
                        // write directory out to console log
                        System.out.println ( "Directory: " + grabDirs[i] ) ;
                    }
                    else if ( grabDirs[i].isFile())
                    {   // write directory path string out to client
                        out.println( "<p><a href=" + grabDirs[i] + ">" +
                                grabDirs[i] + "</a></P>");
                        // write directory out to console log
                        System.out.println ( "File: " + grabDirs[i] +
                                " (" + grabDirs[i] + ")" ) ;
                    }
                }
            }

            sock.close(); // End this socket so it's not listening anymore
        } catch (IOException x) {
            System.out.println("Connection reset. Listening again...");
        }
    }

    // I attribute this function's overall design via courtesy of
    // http://cs.au.dk/~amoeller/WWW/javaweb/server.html.
    // Recieve OutputStream object and InputStream file object so
    // data can be read from file and converted to bytes being written
    // to client side
    public static void convertPush(InputStream file, OutputStream out)
    {
        try { // Create new buffer
            byte[] buffer = new byte[1000];
            // iterate through loop writing to buffer
            while (file.available()>0)
                // send bytes through socket
                out.write(buffer, 0, file.read(buffer));
        } catch (IOException e) { System.err.println(e); }
    }

}


/**********************************************************************
 * My web server responds to requests from a browser/client based on files
 * with extensions: .txt, .html, and .java.
 * The server dynamically creates
 * data by traversing directories on the user's local computer pulling
 * and placing this data into html hyperlink strings. These strings are then
 * converted into byte code for the purpose of being sent back to the client/
 * browser to be displayed to the end user.
 * It also parses user input from the client/browser for adding the sum of two
 * numbers and returns this result to the user via a web form.
 ***********************************************************************/
public class MyWebServer
{   // static variable for keeping while look running.
    public static boolean controlSwitch = true;

    public static void main(String a[]) throws IOException {
        int q_len = 6; /* Limit amount of requests server accepts to 6 */
        int port = 2540; // Client is connecting to server on port 2540
        Socket sock;

        // Create socket connection
        ServerSocket servsock = new ServerSocket(port, q_len);

        System.out.println("Josh's web server running at 2540.\n");
        while (controlSwitch) {
            // sit patiently for new client to connect
            sock = servsock.accept();
            new WebServerWorker (sock).start(); // Launch worker thread to do work
            // Make thread wait for longer connection time.
            try{Thread.sleep(100);} catch(InterruptedException ex) {}
        }
    }
}