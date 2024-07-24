package some.project.v2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ServerExample {

    public static final int SERVER_PORT = 6162;

    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(SERVER_PORT));

        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            if (socketChannel != null) {
                socketChannel.configureBlocking(false);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                while (true) {
                    buffer.clear();
                    int read = socketChannel.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    if (read > 0) {
                        final byte[] bytes = new byte[read];
                        System.arraycopy(buffer.array(), 2, bytes, 0, read);
                        socketChannel.write(ByteBuffer.wrap(new String(bytes).toUpperCase().getBytes(StandardCharsets.UTF_8)));
                    }
                    buffer.flip();
                }
                serverSocketChannel.close();
            }
        }
    }
}
