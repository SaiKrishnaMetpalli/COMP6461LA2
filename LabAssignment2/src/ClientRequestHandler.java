import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;

public class ClientRequestHandler implements Runnable {

	private Socket client_socket;
	private boolean print_Debug_Message;
	private String directory_Path;
	static BufferedWriter b_writer = null;
	static BufferedReader b_reader = null;

	private String header = null;
	private String file_path = null;

	public ClientRequestHandler(Socket client, boolean print_Debug_Message, String directory_Path) {

		this.client_socket = client;
		this.print_Debug_Message = print_Debug_Message;
		this.directory_Path = directory_Path;

	}

	@Override
	public void run() {
		try {
			// read from socket to ObjectInputStream object
			b_reader = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
			b_writer = new BufferedWriter(new OutputStreamWriter(client_socket.getOutputStream()));

			// read request of client
			read_request(b_reader);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void read_request(BufferedReader b_reader) throws Exception {
		String line;
		while ((line = b_reader.readLine()) != null) {

			if (print_Debug_Message == true) {
				System.out.println(line);
			}

			// deal with header
			if (header.isEmpty()) {
				if (line.substring(0, 3).equals("GET")) {
					header = "GET";
				} else if (line.substring(0, 4).equals("POST")) {
					header = "POST";
				}
			}

			int path_start_index = line.indexOf("/", 7); // start searching "/" after "http://"
			if (line.substring(path_start_index).toLowerCase().contains("get")
					|| line.substring(path_start_index).toLowerCase().contains("post")) {

				int index_of_header_in_path;
				if (line.substring(path_start_index).toLowerCase().contains("get")) {
					index_of_header_in_path = line.toLowerCase().indexOf("get");
					
				} else {
					index_of_header_in_path = line.toLowerCase().indexOf("post");
				}
				
				//if is has ..get/ or ..post/ then start searching after / of get or post
				path_start_index = line.indexOf("/", index_of_header_in_path + 4);			//length of post is 4	
				
			}

			file_path = line.substring(path_start_index, line.indexOf(" ", path_start_index + 1));

		}

	}

}
