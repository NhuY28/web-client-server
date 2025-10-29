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

    // mỗi instance sẽ có 1 socket kết nối client
    private Socket connect;

    // server-wide
    private static ServerSocket serverSocket;
    private static volatile boolean isRunning = false;
    private static Thread acceptThread;

    // Không giữ reference GUI ở đây dưới dạng static khi tải lớp
    private static void log(String msg) {
        // in console luôn
        System.out.println(msg);
        // nếu GUI đã khởi (singleton) thì show lên GUI
        try {
            ServerGUI.getInstance().log(msg);
        } catch (Exception ignored) {
            // nếu GUI chưa tạo được thì bỏ qua
        }
    }

    public httpServer(Socket clientSocket) {
        this.connect = clientSocket;
    }

    // start server được GUI gọi
    public static synchronized void startServer() {
        if (isRunning) {
            log("⚠️ Server is already running.");
            return;
        }

        try {
            serverSocket = new ServerSocket(PORT, 0, InetAddress.getByName("0.0.0.0"));
        } catch (IOException e) {
            log("❌ Không thể mở ServerSocket: " + e.getMessage());
            return;
        }

        isRunning = true;
        log("🚀 Server started. Listening on port: " + PORT);

        acceptThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Socket client = serverSocket.accept();
                    if (client != null) {
                        String clientIP = client.getInetAddress().getHostAddress();
                        log("🔗 Client connected: IP: " + clientIP + " (" + new Date() + ")");
                        httpServer worker = new httpServer(client);
                        Thread t = new Thread(worker);
                        t.start();
                    }
                } catch (SocketException se) {
                    // xảy ra khi serverSocket.close() gọi từ stopServer()
                    log("🛑 ServerSocket closed, accept loop ending.");
                    break;
                } catch (IOException ioe) {
                    if (isRunning) {
                        log("❌ IOException in accept loop: " + ioe.getMessage());
                    }
                    break;
                }
            }
            isRunning = false;
            log("🧱 Server accept thread finished.");
        }, "HTTP-Accept-Thread");

        acceptThread.start();
    }

    // stop server được GUI gọi
    public static synchronized void stopServer() {
        if (!isRunning) {
            log("⚠️ Server is not running.");
            return;
        }

        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // sẽ gây SocketException trong accept() và thoát vòng lặp
            }
        } catch (IOException e) {
            log("⚠️ Error closing server socket: " + e.getMessage());
        }

        // optional: interrupt acceptThread
        if (acceptThread != null) {
            acceptThread.interrupt();
        }

        log("🛑 Stop requested for server.");
    }

    // để chạy độc lập từ command-line nếu cần
    public static void main(String[] args) {
        startServer();
    }

    @Override
    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;

        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            String input = in.readLine();
            if (input == null || input.isEmpty()) return;

            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase();
            String fileRequested = parse.nextToken().toLowerCase();

            // --- Xử lý GET /index.html ---
            if (method.equals("GET") && (fileRequested.equals("/") || fileRequested.equals("/index.html"))) {
                File file = new File(WEB_ROOT, DEFAULT_FILE);
                if (file.exists()) {
                    StringBuilder html = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            html.append(line).append("\n");
                        }
                    }

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

                log("📥 Dữ liệu form: " + formData);

                DatabaseConnect.insertStudent(
                        params.get("id"),
                        params.get("name"),
                        params.get("major"),
                        Double.parseDouble(params.get("gpa"))
                );

                // ghi log ra GUI (nếu có)
                try {
                    ServerGUI.getInstance().logWithIcon("src/Server/add.png", "✅ Đã thêm sinh viên vào CSDL: " + params.get("name"));
                } catch (Exception e) {
                    log("✅ Đã thêm sinh viên vào CSDL: " + params.get("name"));
                }

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

                log("🗑️ Yêu cầu xóa sinh viên ID: " + id);

                DatabaseConnect.deleteStudent(id);

                try {
                    ServerGUI.getInstance().logWithIcon("src/Server/delete.png", "✅ Đã xóa sinh viên ID: " + id);
                } catch (Exception e) {
                    log("Đã xóa sinh viên ID: " + id);
                }

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
            log("❌ Server error: " + ioe);
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (dataOut != null) dataOut.close();
                if (connect != null && !connect.isClosed()) connect.close();
            } catch (Exception e) {
                log("⚠️ Error closing stream: " + e.getMessage());
            }

            if (verbose) {
                log("🔒 Connection closed.\n");
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

        log("❌ File " + fileRequested + " not found.");
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
