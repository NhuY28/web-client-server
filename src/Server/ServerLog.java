package Server;

import javax.swing.*;

public class ServerLog {
    public static JTextArea logArea;

    public static void log(String message) {
        if (logArea != null) {
            logArea.append(message + "\n");
        }
        System.out.println(message); // vẫn log ra console nếu cần
    }
}
