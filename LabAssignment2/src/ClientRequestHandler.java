import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;

public class ClientRequestHandler implements Runnable {

	private Socket client_socket;
	private boolean print_Debug_Message;
	private String directory_Path;
	// BufferedWriter b_writer = null;
	private BufferedReader b_reader;
	private BufferedWriter b_writer;

	private String header = "";
	private String file_path = null;
	private String file_name = null;

	private String contentType = "";
	private String contentDisposition = "";

	// deal with request
	private boolean listAllFiles = false;


	// generate respond
	// private String contentType = "plain/text";
	private int statusCode = 200;
	private StringBuilder respond = new StringBuilder();
	private StringBuilder respondBody = new StringBuilder();

	private int contentLength = 0;

	public ClientRequestHandler(Socket client, boolean print_Debug_Message, String directory_Path, BufferedReader br,
			BufferedWriter bw) {

		this.client_socket = client;
		this.print_Debug_Message = print_Debug_Message;
		this.directory_Path = directory_Path;
		this.b_reader = br;
		this.b_writer = bw;

	}

	@Override
	public void run() {
		try {

			// read request of client
			read_request();
			// Dealing with request
			if (print_Debug_Message == true) {
				System.out.println("\n-------------------- Dealing with request... -----------------\n");
				System.out.println("Method:              " + header);
			}

			dealWithRequest();

			// Generate responds
			if (print_Debug_Message)
				System.out.println("\n-------------------- Generate respond... ---------------------\n");
			generateRespond();

			// send responds to client
			if (print_Debug_Message)
				System.out.println("\n-------------------- Send respond to client... ---------------\n");

			b_writer.write(respond.toString() + "\n");

			b_writer.flush();

			if (print_Debug_Message)
				System.out.println("\n-------------------- Finish... -------------------------------\n");
			// Thread.sleep(1000);
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

			if (print_Debug_Message == true) {
				System.out.println("Current Line from response: " + line);
			}
			
			if(line.equals("")) {
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
					file_path = directory_Path;
					//System.out.println("\nListing all Files in directory...");
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
			//System.out.println("\nEnd itr of while loop\n");
			
		}
		// Thread.sleep(1000);

	}

	private void dealWithRequest() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		if (200 != statusCode) return;
        if (file_path.length() > 3 && (file_path.substring(0, 4).equals("/../") || (file_path.substring(0, 4).equals("./..") ))) {
            statusCode = 403;
            respondBody.append("You tried to leave the working directory, which is not allowed for security reason.\r\n");
            return;
        }
        
		if (listAllFiles == true) {
			// print all files in the current directory

			
			print_all_files(file_path, respondBody);
			

		} else {
			// TODO: when pass is ./ i.e. localhost/././file_1
			File getFile = new File(directory_Path + file_path.trim());

			String fileType = getFile.toURL().openConnection().getContentType();
			contentType = fileType;
			if (contentType.equals("text/plain")) {
				contentDisposition = "inline";
			} else {
				contentDisposition = "attachment; filename=" + directory_Path + file_path + ";";
			}
			if (print_Debug_Message) {
				System.out.println("Content Type:        " + fileType);
				System.out.println("Content Disposition: " + contentDisposition);
			}
			if (!contentType.equals("text/plain")) {
				respondBody.append("File type is ").append(contentType).append(", cannot read byte file.");
			} else if (getFile.exists() && getFile.isFile()) {

				BufferedReader getFileContents = new BufferedReader(new FileReader(getFile));
				String getLine;
				while (null != (getLine = getFileContents.readLine())) {
					respondBody.append(getLine).append("\r\n");
				}
			} else {
				System.out.println("\nFile exist status: " + getFile.exists());
				statusCode = 404;
			}
		}
		// Thread.sleep(1000);

	}

	private void print_all_files(String file_path, StringBuilder respondBody) {
		File curDir = new File(file_path);
		File[] filesList = curDir.listFiles();
		

		if (null != filesList) {
			for (File file : filesList) {
				//System.out.println("\nFile: "+ file.getName());
				if (file.isFile()) {

					respondBody.append(file.getName()).append("\r\n");

				}
			}
		}
	}

	private void generateRespond() throws Exception {
		if (404 == statusCode) {
			respond.append("HTTP/1.1 404 NOT FOUND\r\n");
			respondBody.append("The requested File was not found on the server.\r\n");
			// respondBody.append("If you entered the URL manually, please check you
			// spelling and try again.\r\n");
		} else if (403 == statusCode) {
			respond.append("HTTP/1.1 403 Forbidden\r\n");
		} else if (400 == statusCode) {
			respond.append("HTTP/1.1 40 Bad Request\r\n");
		} else {
			// Thread.sleep(1000);
			respond.append("HTTP/1.1 200 OK\r\n");
			// TODO: show create/overwrite info
			if (header.equals("POST"))
				respondBody.append("Post file successfully.");
		}
		respond.append("Connection: close\r\n");
		respond.append("Server: httpfs\n");
		respond.append("Date: ").append(Calendar.getInstance().getTime().toString()).append("\r\n");
		respond.append("Content-Type: ").append(contentType).append("\r\n");
		respond.append("Content-Length: ").append(respondBody.length()).append("\r\n");

		respond.append(respondBody.toString());

	}

}
