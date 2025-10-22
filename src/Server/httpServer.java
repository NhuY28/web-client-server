package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class httpServer implements Runnable {
    static final File WEB_ROOT = new File("src/webroot");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final int PORT = 8080;
    static final boolean verbose = true;

    private Socket connect;

    public httpServer(Socket c) {
        this.connect = c;
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT, 0, InetAddress.getByName("0.0.0.0"))) {

            System.out.println("✅ Server started.\nListening on port: " + PORT + " ...\n");

            while (true) {
                httpServer server = new httpServer(serverSocket.accept());
                if (verbose) {
                    System.out.println("Connection opened (" + new Date() + ")");
                }

                Thread thread = new Thread(server);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("❌ Server connection error: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            String input = in.readLine();
            if (input == null || input.isEmpty()) return;

            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase();
            fileRequested = parse.nextToken().toLowerCase();

            // --- Xử lý GET index.html ---
            if (method.equals("GET") && (fileRequested.equals("/") || fileRequested.equals("/index.html"))) {
                File file = new File(WEB_ROOT, DEFAULT_FILE);
                if (file.exists()) {
                    // Đọc file HTML
                    StringBuilder html = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            html.append(line).append("\n");
                        }
                    }

                    // Lấy dữ liệu sinh viên từ CSDL
                    List<Map<String, String>> students = DatabaseConnect.getAllStudents();
                    StringBuilder tableRows = new StringBuilder();
                    for (Map<String, String> s : students) {
                        tableRows.append("<tr>")
                                .append("<td>").append(s.get("id")).append("</td>")
                                .append("<td>").append(s.get("name")).append("</td>")
                                .append("<td>").append(s.get("major")).append("</td>")
                                .append("<td>").append(s.get("gpa")).append("</td>")
                                .append("<td>")
                                .append("<form action='/delete' method='POST' style='display:inline;'>")
                                .append("<input type='hidden' name='id' value='").append(s.get("id")).append("'>")
                                .append("<button type='submit' style='background:red;color:white;border:none;padding:5px 10px;border-radius:5px;'>Xóa</button>")
                                .append("</form>")
                                .append("</td>")
                                .append("</tr>\n");
                    }


                    // Chèn dữ liệu vào tbody
                    String htmlContent = html.toString().replace("<!-- Dữ liệu sinh viên sẽ hiển thị ở đây -->", tableRows.toString());
                    byte[] data = htmlContent.getBytes("UTF-8");

                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html; charset=UTF-8");
                    out.println("Content-Length: " + data.length);
                    out.println();
                    out.flush();

                    dataOut.write(data);
                    dataOut.flush();
                    return;
                } else {
                    fileNotFound(out, dataOut, fileRequested);
                }
            }

            // --- Xử lý POST /add ---
            else if (method.equals("POST") && fileRequested.equals("/add")) {
                int contentLength = 0;
                String line;
                while (!(line = in.readLine()).isEmpty()) {
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.split(":")[1].trim());
                    }
                }

                char[] body = new char[contentLength];
                in.read(body);
                String formData = new String(body);
                Map<String, String> params = parseFormData(formData);

                System.out.println("📥 Dữ liệu form: " + formData);

                DatabaseConnect.insertStudent(
                        params.get("id"),
                        params.get("name"),
                        params.get("major"),
                        Double.parseDouble(params.get("gpa"))
                );

                String response = """
                        <html><body>
                        <h1>✅ Thêm sinh viên thành công!</h1>
                        <a href='/'>Quay lại trang chủ</a>
                        </body></html>
                        """;

                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html; charset=UTF-8");
                out.println("Content-Length: " + response.getBytes().length);
                out.println();
                out.flush();

                dataOut.write(response.getBytes());
                dataOut.flush();
                return;
            }

            // --- Xử lý POST /delete ---
            else if (method.equals("POST") && fileRequested.equals("/delete")) {
                int contentLength = 0;
                String line;
                while (!(line = in.readLine()).isEmpty()) {
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.split(":")[1].trim());
                    }
                }

                char[] body = new char[contentLength];
                in.read(body);
                String formData = new String(body);
                Map<String, String> params = parseFormData(formData);
                String id = params.get("id");

                System.out.println("🗑️ Yêu cầu xóa sinh viên ID: " + id);

                DatabaseConnect.deleteStudent(id);

                String response = """
            <html><body>
            <h1>🗑️ Đã xóa sinh viên thành công!</h1>
            <a href='/'>Quay lại trang chủ</a>
            </body></html>
            """;

                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html; charset=UTF-8");
                out.println("Content-Length: " + response.getBytes().length);
                out.println();
                out.flush();

                dataOut.write(response.getBytes());
                dataOut.flush();
                return;
            }


        } catch (IOException ioe) {
            System.err.println("❌ Server error: " + ioe);
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (dataOut != null) dataOut.close();
                connect.close();
            } catch (Exception e) {
                System.err.println("Error closing stream: " + e.getMessage());
            }

            if (verbose) {
                System.out.println("🔒 Connection closed.\n");
            }
        }
    }

    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int fileLength = (int) file.length();
        byte[] fileData = new byte[fileLength];
        try (FileInputStream fileIn = new FileInputStream(file)) {
            fileIn.read(fileData);
        }

        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: Java HTTP Server");
        out.println("Date: " + new Date());
        out.println("Content-type: text/html");
        out.println("Content-length: " + fileLength);
        out.println();
        out.flush();

        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();

        if (verbose) System.out.println("❌ File " + fileRequested + " not found.");
    }

    private static Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2)
                params.put(URLDecoder.decode(keyValue[0], "UTF-8"),
                        URLDecoder.decode(keyValue[1], "UTF-8"));
        }
        return params;
    }
}
