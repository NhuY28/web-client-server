package Client;

import java.io.*;
import java.net.*;

public class httpClient {
    public static void main(String[] args) {
        try {
            URL url = new URL("http://localhost:8080/index.html");
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
