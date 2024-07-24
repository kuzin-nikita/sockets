package some.project.v4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerExample {

    public static void main(String[] args) throws IOException {
        List<Command> commands = new ArrayList<>();
        try (ServerSocket serverSocket = new ServerSocket(6070)) {
            commands.add(new EchoCommand());
            commands.add(Command.newCommand("SHUTDOWN", (client, arg) -> {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
            ConcurrentMap<UUID, User> users = new ConcurrentHashMap<>();
            commands.add(Command.newCommand("LOGIN", (client, username) -> {
                if (username == null) {
                    client.sendReply("WRONG SYNTAX");
                    return;
                }
                users.values().forEach(it -> it.getClient().sendReply("NEW USER " + username));
                users.put(client.getUuid(), new User(username, client));
                client.sendReply("HELLO, " + username);
            }));
            commands.add(Command.newCommand("MESSAGE", (client, commandArgs) -> {
                if (commandArgs == null) {
                    client.sendReply("WRONG SYNTAX");
                    return;
                }
                final String[] commandParts = commandArgs.trim().split("\\s+", 2);
                if (commandParts.length < 2) {
                    client.sendReply("WRONG SYNTAX");
                } else {
                    String toClientName = commandParts[0];
                    User user = users.get(client.getUuid());
                    if (user == null) {
                        client.sendReply("YOU ARE NOT REGISTERED");
                        return;
                    }
                    Optional<User> toUserOpt = users.values().stream().filter(it -> it.name.equals(toClientName)).findFirst();
                    toUserOpt.ifPresentOrElse(toUser -> {
                        Client toClient = toUser.getClient();
                        String message = commandParts[1];
                        toClient.sendReply(user.getName() + ": " + message);
                        client.sendReply("SUCCESS");
                    }, () -> client.sendReply("USER " + toClientName + " NOT FOUND"));
                }
            }));
            new Server(serverSocket, commands).run();
        }
    }

    private static class User {
        private final String name;
        private final Client client;

        private User(String name, Client client) {
            this.name = name;
            this.client = client;
        }

        public String getName() {
            return name;
        }

        public Client getClient() {
            return client;
        }
    }

    public static class EchoCommand implements Command {

        @Override
        public String getName() {
            return "UPPER_ECHO";
        }

        @Override
        public void process(Client client, String arg) {
            client.sendReply(arg.toUpperCase());
        }
    }

    private interface Command {
        String getName();

        void process(ServerExample.Client client, String arg);

        String COMMAND_NAME_REGEX = "^[A-Z0-9_]+$";

        static Command newCommand(String name, BiConsumer<ServerExample.Client, String> process) {
            if (!name.matches(COMMAND_NAME_REGEX)) {
                throw new IllegalArgumentException(name + " is not correct.");
            }
            return new Command() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public void process(ServerExample.Client client, String arg) {
                    process.accept(client, arg);
                }
            };
        }
    }

    private static class Server implements Runnable, Closeable {

        private final ServerSocket serverSocket;
        private final Map<String, Command> commands;

        private Server(ServerSocket serverSocket, List<Command> commands) {
            this.serverSocket = serverSocket;
            this.commands = commands.stream().collect(Collectors.toMap(Command::getName, Function.identity()));
        }

        @Override
        public void run() {
            try (serverSocket) {
                //noinspection InfiniteLoopStatement
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    try {
                        Thread thread = new Thread(new ClientProcessor(clientSocket, commands));
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

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }

    private static class Client implements AutoCloseable {
        private final Socket clientSocket;
        private final BufferedReader socketBufferedReader;
        private final BufferedWriter socketBufferedWriter;
        private final UUID uuid;

        Client(final Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                socketBufferedReader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                socketBufferedWriter = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            this.uuid = UUID.randomUUID();
        }

        public UUID getUuid() {
            return uuid;
        }

        @Override
        public void close() throws IOException {
            clientSocket.close();
        }

        public synchronized void sendReply(final String replyText) {
            try {
                System.out.println("SEND MESSAGE \"" + replyText + "\"");
                socketBufferedWriter.write(replyText);
                socketBufferedWriter.newLine();
                socketBufferedWriter.flush();
            } catch (final IOException ioException) {
                throw new IllegalStateException(ioException);
            }
        }

        String waitAndGetLine() throws IOException {
            return socketBufferedReader.readLine();
        }

        public boolean isClosed() {
            return clientSocket.isClosed();
        }
    }

    private static class ClientProcessor implements Runnable {

        private final Socket socket;
        private final Map<String, Command> commands;

        public ClientProcessor(Socket socket, Map<String, Command> commands) {
            this.socket = socket;
            this.commands = commands;
        }

        @Override
        public void run() {
            try {
                ServerExample.Client client = new ServerExample.Client(socket);
                client.sendReply("HELLO");
                while (!client.isClosed()) {
                    String line = client.waitAndGetLine();
                    if (line == null) {
                        return;
                    }
                    final String[] commandParts = line.trim().split("\\s+", 2);
                    final String commandName = commandParts[0].toUpperCase();
                    Command command = commands.get(commandName);
                    if (command != null) {
                        command.process(client, commandParts.length > 1 ? commandParts[1] : null);
                    } else {
                        System.out.println("Unknown command " + commandName);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
