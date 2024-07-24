package some.project.v1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientSocketExample {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", ServerExample.SERVER_PORT);
        final InputStream inputStream = socket.getInputStream();
        final OutputStream outputStream = socket.getOutputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        bufferedWriter.write("hello");
        bufferedWriter.newLine();
        bufferedWriter.flush();
        System.out.println(bufferedReader.readLine());
        bufferedWriter.write("some test");
        bufferedWriter.newLine();
        bufferedWriter.flush();
        System.out.println(bufferedReader.readLine());
        System.out.println(bufferedReader.readLine());
    }
}
