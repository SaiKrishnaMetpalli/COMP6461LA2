import java.net.Socket;
import java.util.Calendar;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ClientRequestHandler implements Runnable {

	private Socket client_socket;
	private boolean is_debug_on;
	private String dir_path;
	private BufferedReader b_reader;
	private BufferedWriter b_writer;

	private String header = "";
	private String file_path = null;

	private String contentType = "";
	private String contentDisposition = "";

	// deal with request
	private boolean listAllFiles = false;

	private int statusCode = 200;
	private StringBuilder respond = new StringBuilder();
	private StringBuilder main_response_data = new StringBuilder();

	private int contentLength = 0;

	public ClientRequestHandler(Socket client, boolean print_Debug_Message, String directory_Path, BufferedReader br,
			BufferedWriter bw) {

		this.client_socket = client;
		this.is_debug_on = print_Debug_Message;
		this.dir_path = directory_Path;
		this.b_reader = br;
		this.b_writer = bw;

	}

	@Override
	public void run() {
		try {

			// read request of client
			read_request();
			// Processing the request
			if (is_debug_on == true) {
				System.out.println("\n---------- Processing the request...\n");
				System.out.println("Type of the method : " + header);
			}

			process_request();

			// making response for request
			if (is_debug_on)
				System.out.println("\n---------- Making response...\n");
			make_response();

			// sending response to the client
			if (is_debug_on)
				System.out.println("\n---------- Sending response to the client...\n");

			b_writer.write(respond.toString() + "\n");

			b_writer.flush();

			if (is_debug_on)
				System.out.println("\n----------response sent successfully.\n");

			b_writer.close();
			client_socket.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void read_request() throws Exception {
		String line;

		while ((line = b_reader.readLine()) != null) {

			if (is_debug_on == true) {
				System.out.println("Current Line from response: " + line);
			}

			if (line.equals("")) {
				break;
			}
			// deal with header
			if (line.toLowerCase().contains("get")) {
				header = "get";

				int path_start_index = line.indexOf("/"); // start searching "/" after "http://" in "GET /files.txt
															// HTTP/1.0" (our received line) should return 4

				int index_of_space = line.indexOf(" ", path_start_index);
				file_path = line.substring(path_start_index, index_of_space);

				if ("/".equals(file_path.trim())) {
					file_path = dir_path;
					// System.out.println("\nListing all Files in directory...");
					listAllFiles = true;
					return;
				}

			}
//
//			else if (line.contains("Content-Length")) {
//				int index_of_content_len = line.indexOf("Content-Length");
//				try {
//					contentLength = Integer
//							.parseInt(line.substring(index_of_content_len + "Content-Length".length() + 1));
//				} catch (Exception e) {
//					System.out.println("Exception generated in parsing Content-Length!");
//				}
//
//			}
			// System.out.println("\nEnd itr of while loop\n");

		}

	}

	private void process_request() throws IOException, InterruptedException {

		if (200 != statusCode)
			return;
		if (file_path.length() > 3
				&& ((file_path.substring(0, 4).equals("./..") || file_path.substring(0, 4).equals("/../")))) {
			statusCode = 403;
			main_response_data.append(
					"Due to security reasons, It is not allowed to access other directories on this server!\r\n");
			return;
		}

		if (listAllFiles == true) {

			list_of_all_files(file_path, main_response_data);

		} else {

			File requested_file = new File(dir_path + file_path.trim());

			contentType = requested_file.toURL().openConnection().getContentType();

			if (contentType.equals("text/plain")) {
				contentDisposition = "inline";
			} else {
				contentDisposition = "attachment; filename=" + dir_path + file_path + ";";
			}
			if (is_debug_on) {
				System.out.println("Content Disposition: " + contentDisposition);
				System.out.println("Content Type:        " + contentType);

			}
			if (!contentType.equals("text/plain")) {
				main_response_data.append("Cannot read").append(contentType).append(", type of file!");
			} else if (requested_file.exists() && requested_file.isFile()) {

				BufferedReader data_of_file = new BufferedReader(new FileReader(requested_file));
				String getLine;
				while (null != (getLine = data_of_file.readLine())) {
					main_response_data.append(getLine).append("\r\n");
				}
			} else {
				System.out.println("\nFile exist status: " + requested_file.exists());
				statusCode = 404;
			}
		}

	}

	private void list_of_all_files(String file_path, StringBuilder respondBody) {
		File current_directory = new File(file_path);
		File[] filesList = current_directory.listFiles();
		if (null != filesList) {
			for (File file : filesList) {
				if (file.isFile()) {
					respondBody.append(file.getName()).append("\r\n");
				}
			}
		}
	}

	private void make_response() throws Exception {
		if (statusCode == 404) {
			respond.append("HTTP/1.1 404 NOT FOUND\r\n");
			main_response_data.append("The requested File was not found on the server.\r\n");
			// respondBody.append("If you entered the URL manually, please check you
			// spelling and try again.\r\n");
		} else if (statusCode == 403) {
			respond.append("HTTP/1.1 403 Forbidden\r\n");
		} else if (statusCode == 400) {
			respond.append("HTTP/1.1 40 Bad Request\r\n");
		} else {
			// Thread.sleep(1000);
			respond.append("HTTP/1.1 200 OK\r\n");
			// TODO: show create/overwrite info
			if (header.equals("POST"))
				main_response_data.append("Post file successfully.");
		}
		respond.append("Connection: close\r\n");
		respond.append("Server: httpfs\n");
		respond.append("Date: ").append(Calendar.getInstance().getTime().toString()).append("\r\n");
		respond.append("Content-Type: ").append(contentType).append("\r\n");
		respond.append("Content-Length: ").append(main_response_data.length()).append("\r\n");

		respond.append(main_response_data.toString());

	}

}
