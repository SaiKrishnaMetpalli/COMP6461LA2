import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HttpServerLibrary {
	private String[] cmd_Arguments;
	private int port_No;
	private boolean print_Debug_Message;
	private String directory_Path;

	public HttpServerLibrary(String[] arguments) {
		// DEFAULT VALUES FOR SERVER
		cmd_Arguments = arguments;
		port_No = 8080;
		print_Debug_Message = false;
		directory_Path = ".";
	}

	public void handleCommand() {
		boolean is_Continue = true;
		for (int i = 0; i < cmd_Arguments.length; i++) {
			switch (cmd_Arguments[i]) {
			case "help":
				StringBuilder httpfs_Help_Txt = new StringBuilder("\nhttpfs is a simple file server.\n\n");
				httpfs_Help_Txt.append("usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]\n\n");
				httpfs_Help_Txt.append("    -v Prints debugging messages.\n");
				httpfs_Help_Txt.append("    -p Specifies the port number that the server will listen and serve at.\n");
				httpfs_Help_Txt.append("       Default is 8080.\n");
				httpfs_Help_Txt.append("    -d Specifies the directory that the server will use to read/write\n");
				httpfs_Help_Txt.append("       requested files. Default is the current directory when launching the\n");
				httpfs_Help_Txt.append("       application.\n\n");
				System.out.println(httpfs_Help_Txt.toString());
				is_Continue = true;
				break;
			case "-v":
				print_Debug_Message = true;
				is_Continue = true;
				break;
			case "-p":
				if (++i < cmd_Arguments.length) {
					try {
						port_No = Integer.parseInt(cmd_Arguments[i]);
						is_Continue = true;
					} catch (Exception exception) {
						System.out.println(cmd_Arguments[i] + " port number has to be an integer");
						is_Continue = false;
					}
				} else {
					System.out.println("\n==========Port number not found");
					is_Continue = false;
				}
				break;
			case "-d":
				if (++i < cmd_Arguments.length) {
					directory_Path = cmd_Arguments[i];
					is_Continue = true;
				} else {
					System.out.println("\n==========Directory path not found");
					is_Continue = false;
				}
				break;
			default:
				System.out.println(cmd_Arguments[i] + " command not found");
				is_Continue = false;
				break;
			}
		}

		if (is_Continue) {
			try {
				ServerSocket serverSocket = new ServerSocket(port_No);
				while (true) {
					Socket client = serverSocket.accept();
					BufferedWriter b_writer = new BufferedWriter(
							new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
					BufferedReader b_reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
					
					ClientRequestHandler crh = new ClientRequestHandler(client, print_Debug_Message, directory_Path,
							b_reader, b_writer);
					Thread t = new Thread(crh);
					t.start();
				}
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			}
		}

	}
}
