package Server;

import javax.swing.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.net.URL;

public class ServerGUI {
    private JFrame frame;
    private JTextPane logPane;
    private JScrollPane scrollPane;
    private JButton clearButton, startButton, stopButton;
    private static ServerGUI instance;

    private ServerGUI() {
        frame = new JFrame("HTTP Server Log Viewer");
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Đặt icon cho cửa sổ (nếu có)
        try {
            URL iconURL = getClass().getResource("/Server/server_icon.png");
            if (iconURL != null) {
                ImageIcon icon = new ImageIcon(iconURL);
                frame.setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Không tìm thấy server_icon.png");
        }

        // JTextPane hiển thị HTML (hỗ trợ icon trong log)
        logPane = new JTextPane();
        logPane.setContentType("text/html");
        logPane.setEditable(false);
        logPane.setText("<html><body style='font-family:Consolas; font-size:14px; color:black; background-color:white;'></body></html>");

        scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Server Logs"));
        frame.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        startButton = new JButton("▶ Start Server");
        stopButton  = new JButton("⏹ Stop Server");
        clearButton = new JButton("🧹 Clear Log");

        startButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        stopButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        startButton.setFocusPainted(false);
        stopButton.setFocusPainted(false);
        clearButton.setFocusPainted(false);

        startButton.setBackground(new Color(220, 255, 220));
        stopButton.setBackground(new Color(255, 220, 220));
        clearButton.setBackground(new Color(240, 240, 240));

        startButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        stopButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        startButton.addActionListener(e -> {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            logWithIcon(null, "🚀 Start button pressed. Starting server...");
            httpServer.startServer();
        });

        stopButton.addActionListener(e -> {
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
            logWithIcon(null, "⏹ Stop button pressed. Stopping server...");
            httpServer.stopServer();
        });

        clearButton.addActionListener(e -> clearLogs());

        // initial buttons state
        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(startButton);
        bottomPanel.add(stopButton);
        bottomPanel.add(clearButton);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Singleton
    public static synchronized ServerGUI getInstance() {
        if (instance == null) {
            instance = new ServerGUI();
        }
        return instance;
    }

    // Log plain text (không icon)
    public void log(String message) {
        logWithIcon(null, message);
    }

    // Log có icon (iconPath là đường dẫn file trên disk: ví dụ "src/Server/success.png")
    public void logWithIcon(String iconPath, String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLEditorKit kit = (HTMLEditorKit) logPane.getEditorKit();
                HTMLDocument doc = (HTMLDocument) logPane.getDocument();

                String iconHTML = "";
                if (iconPath != null && !iconPath.isEmpty()) {
                    // dùng file: để load từ filesystem
                    iconHTML = "<img src='file:" + iconPath + "' width='18' height='18' style='vertical-align:middle'/> ";
                }

                // escape message basic (nếu cần nâng cao có thể sanitize)
                String html = "<p style='margin:4px;'>" + iconHTML + message + "</p>";

                kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
                logPane.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                // fallback: in ra console nếu lỗi
                e.printStackTrace();
            }
        });
    }

    private void clearLogs() {
        SwingUtilities.invokeLater(() -> logPane.setText("<html><body style='font-family:Consolas; font-size:14px; color:black; background-color:white;'></body></html>"));
    }

    // tiện: main để chạy GUI độc lập
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::getInstance);
    }
}
