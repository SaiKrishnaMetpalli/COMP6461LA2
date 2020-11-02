
public class httpfs {

	public static void main(String[] args) {
		if (args.length != 0) {
			HttpServerLibrary http_Server = new HttpServerLibrary(args);
			http_Server.handleCommand();
		} else {
			System.out.println("\n==========Invalid Command");
		}

	}

}
