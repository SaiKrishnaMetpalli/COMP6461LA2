import java.net.Socket;
import java.util.Calendar;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ClientRequestHandler implements Runnable {

	private Socket client_Socket;
	private boolean is_Debug_On;
	private String dir_Path;
	private BufferedReader b_Reader;
	private BufferedWriter b_Writer;
	private String method = "";
	private String file_Path = null;
	private String content_Type = "";
	private String content_Disposition = "";
	private boolean list_All_Files = false;
	private boolean is_Overwrite=false;
	private int status_Code = 200;
	private StringBuilder respond = new StringBuilder();
	private StringBuilder main_Response_Data = new StringBuilder();
	String data="";
	
	public ClientRequestHandler(Socket client, boolean print_Debug_Message, String directory_Path, BufferedReader br,
			BufferedWriter bw) {

		this.client_Socket = client;
		this.is_Debug_On = print_Debug_Message;
		this.dir_Path = directory_Path;
		this.b_Reader = br;
		this.b_Writer = bw;

	}

	@Override
	public void run() {
		try {

			// read request of client
			readRequest();
			// Processing the request
			if (is_Debug_On == true) {
				System.out.println("\n---------- Processing the request...\n");
				System.out.println("Type of the method : " + method);
			}

			processRequest();

			// making response for request
			if (is_Debug_On)
				System.out.println("\n---------- Making response...\n");
			makeResponse();

			// sending response to the client
			if (is_Debug_On)
				System.out.println("\n---------- Sending response to the client...\n");

			b_Writer.write(respond.toString() + "\n");

			b_Writer.flush();

			if (is_Debug_On)
				System.out.println("\n----------response sent successfully.\n");

			b_Writer.close();
			client_Socket.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void readRequest() throws Exception {
		String line;

		while ((line = b_Reader.readLine()) != null) {

			if (is_Debug_On == true) {
				System.out.println(line);
			}
			if (line.equals("")) {
				break;
			}
			
			// deal with method
			if (line.toLowerCase().contains("get")) {
				method = "get";
				int path_start_index = line.indexOf("/"); // start searching "/" after "http://" in "GET /files.txt
								// HTTP/1.0" (our received line) should return 4
				int index_of_space = line.indexOf(" ", path_start_index);
				file_Path = line.substring(path_start_index, index_of_space);
			}			
			else if (line.toLowerCase().contains("post")) {
				method = "post";
				int path_start_index = line.indexOf("/"); // start searching "/" after "http://" in "GET /files.txt
								// HTTP/1.0" (our received line) should return 4
				int index_of_space = line.indexOf(" ", path_start_index);
				file_Path = line.substring(path_start_index, index_of_space);
			}			
			
			if ("/".equals(file_Path.trim())) {
				file_Path = dir_Path;
				list_All_Files = true;
				return;
			}			
			
			if((method.equals("post")) && (line.contains("Content-Length"))) {				
				line=b_Reader.readLine();
				if(line.contains("Overwrite")) {
					is_Overwrite=true;
					line=b_Reader.readLine();
				}
				data=line;
				if(is_Debug_On) {
					System.out.println("Storing the data from headers");
				}				
			}
			
		}
	}

	private void processRequest() throws IOException, InterruptedException {
		
		if (200 != status_Code)
			return;
		
		if (file_Path.length() > 3
				&& ((file_Path.substring(0, 4).equals("./..") || file_Path.substring(0, 4).equals("/../")))) {
			status_Code = 403;
			main_Response_Data.append(
					"Due to security reasons, It is not allowed to access other directories on this server!\r\n");
			return;
		}
		
		if (list_All_Files == true) {
			listOfAllFiles(file_Path, main_Response_Data);
		} else {
			if(method.equals("get")) {
				File requested_file = new File(dir_Path + file_Path.trim());

				content_Type = requested_file.toURI().toURL().openConnection().getContentType();

				if (content_Type.equals("text/plain")) {
					content_Disposition = "inline";
					main_Response_Data.append("Content-Disposition: inline").append("\r\n");
				} else {
					content_Disposition = "attachment; filename=" + dir_Path + file_Path + ";";
					main_Response_Data.append("Content-Disposition: ").append(content_Disposition).append("\r\n");
				}
				if (is_Debug_On) {
					System.out.println("Content Disposition: " + content_Disposition);
					System.out.println("Content Type:        " + content_Type);

				}
				if (!content_Type.equals("text/plain")) {
					main_Response_Data.append("Cannot read").append(content_Type).append(", type of file!");
				} else if (requested_file.exists() && requested_file.isFile()) {

					BufferedReader data_of_file = new BufferedReader(new FileReader(requested_file));
					String getLine;
					while (null != (getLine = data_of_file.readLine())) {
						main_Response_Data.append(getLine).append("\r\n");
					}
					data_of_file.close();
				} else {
					System.out.println("\nFile exist status: " + requested_file.exists());
					status_Code = 404;
				}
			} else {
				File requested_file = new File(dir_Path + file_Path.trim());
				PrintWriter out = null;
				if (requested_file.exists() && requested_file.isFile()) {
					out = new PrintWriter(new FileOutputStream(requested_file, true));
				} else {
					out=new PrintWriter(requested_file);
				}
				out.append(data);
				out.close();
			}			
		}
	}

	private void listOfAllFiles(String file_Path, StringBuilder respond_Body) {
		File current_directory = new File(file_Path);
		File[] filesList = current_directory.listFiles();
		if (null != filesList) {
			for (File file : filesList) {
				if (file.isFile()) {
					respond_Body.append(file.getName()).append("\r\n");
				}
			}
		}
	}

	private void makeResponse() throws Exception {
		if (status_Code == 404) {
			respond.append("HTTP/1.1 404 NOT FOUND\r\n");
			main_Response_Data.append("The requested File was not found on the server.\r\n");			
		} else if (status_Code == 403) {
			respond.append("HTTP/1.1 403 Forbidden\r\n");
		} else if (status_Code == 400) {
			respond.append("HTTP/1.1 40 Bad Request\r\n");
		} else {			
			respond.append("HTTP/1.1 200 OK\r\n");			
			if (method.equals("POST"))
				main_Response_Data.append("Post file successfully.");
		}
		
		respond.append("Connection: close\r\n");
		respond.append("Server: httpfs\n");
		respond.append("Date: ").append(Calendar.getInstance().getTime().toString()).append("\r\n");
		respond.append("Content-Type: ").append(content_Type).append("\r\n");
		respond.append("Content-Length: ").append(main_Response_Data.length()).append("\r\n");
		respond.append(main_Response_Data.toString());
	}

}
