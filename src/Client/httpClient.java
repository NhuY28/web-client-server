package Client;

import java.io.*;
import java.net.*;

public class httpClient {
    public static void main(String[] args) {
        try {
//            URL url = new URL("http://10.56.66.163:8080/index.html");
            URL url = new URL("http://172.20.10.7:8080/index.html");
            // URL url = new URL("http://192.168.1.12:8080/index.html");  thay ip của máy chạy server vào

            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            String line;
            System.out.println("📩 Response from server:\n");
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
