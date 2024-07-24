package some.project.v1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerExample {

    public static final int SERVER_PORT = 6162;
    public static int MESSAGES_COUNT = 0;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        System.out.println("Ready to accept data");
        final Socket clientSocket = serverSocket.accept();
        System.out.println("Socket connected");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

        while (!clientSocket.isClosed()) {
            String line = bufferedReader.readLine();
            if (line != null) {
                MESSAGES_COUNT++;
                String response = getResponse(line);
                System.out.println(String.format("Client said: %s", line));
                System.out.println(MESSAGES_COUNT);
                sendData(bufferedWriter, response);
            }
        }
        bufferedReader.close();
        bufferedWriter.close();
        clientSocket.close();
        serverSocket.close();
    }

    private static void sendData(final BufferedWriter bw, final String data) throws IOException {
        bw.append(data);
        bw.newLine();
        bw.flush();
    }

    private static String getResponse(final String request) {
        return String.format("Echo response: %s", request);
    }
}