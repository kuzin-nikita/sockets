package some.project.v3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class ServerExample {

    public static void main(String[] args) {
        new Server(6070).run();
    }

    private static class Server implements Runnable {

        private final int port;

        private Server(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                //noinspection InfiniteLoopStatement
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    try {
                        clientSocket.setSoTimeout((int) TimeUnit.MINUTES.toMillis(1));
                        Thread thread = new Thread(new ClientProcessor(clientSocket, () -> {
                            try {
                                serverSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }));
                        thread.start();
                    } catch (final Exception exception) {
                        clientSocket.close();
                    }
                }
            } catch (SocketException socketException) {
                System.out.println("Server closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ClientProcessor implements Runnable {

        private final Socket socket;
        private final Runnable onShutdown;

        public ClientProcessor(Socket socket, Runnable onShutdown) {
            this.socket = socket;
            this.onShutdown = onShutdown;
        }

        @Override
        public void run() {
            try (socket;
                 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                while (!socket.isClosed()) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        return;
                    }
                    if (line.equalsIgnoreCase("SHUTDOWN")) {
                        bufferedWriter.write("OK");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        onShutdown.run();
                    }
                    bufferedWriter.append(getResponse(line));
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getResponse(String request) {
        return request.toUpperCase();
    }
}
