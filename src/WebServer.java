package src;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class WebServer {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader inputBufferedReader;
    private BufferedWriter outputBufferedWriter;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String contentType;
    private String typeOfFile;

    private static final String PATH_POEM = "./src/poem/";
    private static final String PATH_IMAGES = "./src/images/";
    private static final String ERROR_404 = "./src/404.html";
    private static final String MAIN_PAGE = "./src/main-page.html";
    private static final String URL_CS50 = "https://www.youtube.com/watch?v=LfaMVlDaQ24&ab_channel=freeCodeCamp.org";

    /**
     * This method sets up a server socket on port 8080 and continuously listens for client connections
     * When a client connects, it handles the connection
     */
    public void serverCom() {
        try {
            serverSocket = new ServerSocket(8088);
            System.out.println("Server started. Listening for messages.");
            while (true) {
                handleClientConnection();
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method handles a connection from a client with the GET request
     * Accepts the client socket, sets up streams and reads the client's request
     * Then, parses the requested resource, determines its content type, handles the resource request
     * Sends a 404 error if the resource is not valid and closes the streams and buffers
     */
    private void handleClientConnection() throws IOException {
        clientSocket = serverSocket.accept();
        System.out.println("Client connected. Listening: " + clientSocket.toString());

        setupStreams();

        StringBuilder request = readRequest();
        System.out.println("Request received from the web browser: ");
        System.out.println(request);

        String resource = parseResource(request);
        determineContentType(resource);

        boolean validResourceFound = handleResourceRequest(resource);

        if (!validResourceFound) {
            send404();
        }
        closingStreamAndBuffers();
    }

    /**
     * This method handles GET requests for specific resources
     * Checks the requested resource path and responds accordingly: sending files
     * Returns false if the resource is not recognized.
     */
    private boolean handleResourceRequest(String resource) throws IOException {
        switch (resource) {
            case "/":
                sendFile(MAIN_PAGE);
                return true;
            case "/images/java-logo.png":
            case "/images/javascript-logo.png":
                sendFile(PATH_IMAGES + resource.substring(resource.lastIndexOf("/")));
                return true;
            case "/poem/sonnet-18.html":
            case "/poem/the-new-colossus.html":
                sendFile(PATH_POEM + resource.substring(resource.lastIndexOf("/")));
                return true;
            case "/cs50":
                redirectToYoutube(URL_CS50);
                return true;
            default:
                return false;
        }
    }

    /**
     * Method created to set up the streams and buffers to get input and output from the web browser
     * @throws IOException
     */
    private void setupStreams() throws IOException {
        inputStream = clientSocket.getInputStream();
        outputStream = clientSocket.getOutputStream();
        inputBufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        outputBufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    /**
     * This method reads and constructs a request from the input stream, appending each line
     * Returns the GET request as a StringBuilder
     * @throws IOException
     */
    private StringBuilder readRequest() throws IOException {
        StringBuilder request = new StringBuilder();
        String line = inputBufferedReader.readLine();
        while (!line.isBlank()) {
            request.append(line).append("\n");
            line = inputBufferedReader.readLine();
        }
        System.out.println("---REQUEST----");
        System.out.println(request);
        return request;
    }

    /**
     * This method converts the request into a string and extracts the first line of the GET request
     * It returns the resource by splitting the line by spaces and returns the second element (index 1), which is the HTTP method used in the GET request
     */
    private String parseResource(StringBuilder request) {
        String firstLine = request.toString().split("\n")[0];
        return firstLine.split(" ")[1];
    }

    /**
     * Method created to determine the content type and type of file
     */
    private void determineContentType(String resource) {
        if(resource.startsWith("/test") || resource.startsWith("/images")) {
            contentType = resource.split("\\.")[1]; //slip in the dot so we can the content type
        }
        else{
            contentType = "html"; //for the 404 page and indexDir
        }
        typeOfFile = contentType.equals("png") ? "image/" : "text/"; //if png equals image/, else text/
    }

    /**
     * This method writes HTTP response headers including status code, status message, content type, and content length to an output stream in UTF-8 encoding
     * @throws IOException
     */
    private void writeResponseHeaders(int statusCode, String statusMessage, String contentType, long contentLength) throws IOException {
        outputStream.write(("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: " + contentType + "; charset=UTF-8\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Length: " + contentLength + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("\r\n").getBytes());
    }

    /**
     * This method sends a file specified by its path over an output stream. It first checks if the file exists
     * It reads the file's contents using a FileInputStream, writes appropriate HTTP response headers indicating success (status code 200)
     * Writes the file's content bytes to the output stream and flushes it
     * @throws IOException
     */
    private void sendFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                writeResponseHeaders(200, "Document Follows", typeOfFile + contentType, file.length());
                outputStream.write(fileInputStream.readAllBytes());
                outputStream.flush();
            } catch (SocketException e) {
                if (e.getMessage().equals("Broken pipe")) {
                    System.err.println("Client closed the connection before the file was fully sent.");
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * This method sends a 404 error response along with an error page
     * It reads the contents of the error page file, writes HTTP response headers indicating the error (status code 404 - Not Found)
     * Writes the error page's content bytes to the output stream and flushes it
     * @throws IOException
     */

    private void send404() throws IOException {
        File file404 = new File(ERROR_404);
        try (FileInputStream fileInputStream404 = new FileInputStream(file404)) {
            writeResponseHeaders(404, "Not Found", typeOfFile + contentType, file404.length());
            outputStream.write(fileInputStream404.readAllBytes());
            outputStream.flush();
        }
    }

    /**
     * This method generates a 302 Found HTTP response with a redirection header to a specified YouTube URL
     * Writes this response to the output stream and flushes it to redirect the client's browser to the provided YouTube URL
     * @throws IOException
     */
    private void redirectToYoutube(String youtubeUrl) throws IOException {
        String response = "HTTP/1.1 302 Found \r\nLocation: " + youtubeUrl + "\r\n\r\n";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }

    /**
     * Method created to close the socket, streams and buffers
     * @throws IOException
     */
    private void closingStreamAndBuffers() throws IOException {
        clientSocket.close();
        outputStream.close();
        inputStream.close();
        inputBufferedReader.close();
        outputBufferedWriter.close();
    }

    public static void main(String[] args) {
        WebServer webServer = new WebServer();
        webServer.serverCom();
    }
}

