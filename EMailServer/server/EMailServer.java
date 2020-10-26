package server;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Classe responsavel por instanciar um servidor de e-mail.
 * Nela sao definidos os diretorios raizes do servidor de e-mail utilizado pelo sistema, bem como sao 
 * mantidas listas de usuarios conectados e de seus e-mails, alem de deter conhecimento da porta escolhida 
 * para fornecer conexoes aos usuarios.
 * A classe opera basicamente recebendo (e aceitando) pedidos de conexao de usuarios e abrindo threads de 
 * comunicacao com cada um deles, assim viabilizando trocas de dados com multiplos usuarios simultaneamente
 */
public class EMailServer {
	private int port;
	private Set<String> userEMails = new HashSet<>();
	private Set<UserThread> userThreads = new HashSet<>();
	private final String serverRootPath = "C:\\JavaEMailServer";
	// private final String serverRootPath = "\\Users\\matheusbarbsaveMessageosa\\eclipse-workspace\\EpEmail2\\src\\userData";
	private final String userDirectoryPath = serverRootPath + "\\Users";
	private final String fileDirectoryPath = serverRootPath + "\\Files";

	public EMailServer(int port) {
		this.port = port;
	}

	Set<String> getuserEMails() {
		return this.userEMails;
	}
	public String getUserDirectoryPath(){
		return this.userDirectoryPath;
	}
	public String getFileDirectoryPath(){
		return this.fileDirectoryPath;
	}

	/**
	 * Inicializa a estrutura de diretorios do servidor para recepcionar os e-mails e arquivos enviados pelos 
	 * clientes, sendo criados caso nao existam ainda ou realizando bypass para prevenir erros em tempo de 
	 * execucao e a sobrescrita dos arquivos ali presentes.
	 */
    private void initialize() {
        File serverDirectory = new File(serverRootPath);
		File userDirectory = new File(userDirectoryPath);
		File fileDirectory = new File(fileDirectoryPath);

        if (!serverDirectory.exists()) 
			serverDirectory.mkdirs();
			
		if (!userDirectory.exists())
			userDirectory.mkdirs();

		if (!fileDirectory.exists())
			fileDirectory.mkdirs();
	}

	/**
	 * Viabiliza a criacao de um diretorio especifico para cada usuario de e-mail, sendo este passado por
	 * parametro, promovendo organizacao dos usuarios por pastas distintas.
	 * Tal qual no metodo de inicializacao, firma o contrato de criar tal diretorio somente caso ele nao 
	 * exista previamente.
	 */
	public void makeUserDirectory(String userEmail) {
		File userEmailDirectory = new File(userDirectoryPath + "\\" + userEmail);

		if (!userEmailDirectory.exists())
			userEmailDirectory.mkdirs();
	}

	/**
	 * Instancia o socket do servidor com a porta passada por parametro e inicia laco que aguarda pedidos de 
	 * conexao por parte dos usuarios e, para cada novo usuario, abre uma thread especifica de trocas de dados
	 */
	public void execute() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("**** Welcome to our E-Mail Server! ****");
            System.out.println("\nListening on port: " + port);

			while (true) {
				Socket socket = serverSocket.accept(); 
				System.out.println("New user connected: " + socket.getInetAddress());

				UserThread newUser = new UserThread(socket, this);
				userThreads.add(newUser);
				newUser.start();
			}

		} catch (IOException ex) {
			System.out.println("Error in the server: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * Armazena o e-mail do novo usuario conectado na lista do servidor
	 */
	void addUserEMail(String userEMail) {
		userEMails.add(userEMail);
	}

	/**
	 * Remove um usuario da lista de e-mails e de threads abertas quando um usuario desconecta
	 */
	void removeUser(String userEMail, UserThread user) {
		boolean removed = userEMails.remove(userEMail);
		if (removed) {
			userThreads.remove(user);
			System.out.println("User " + userEMail + " has quitted");
		}
	}
    
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Syntax: java EMailServer <port-number>");
			System.exit(0);
		}

		int port = Integer.parseInt(args[0]);

        EMailServer server = new EMailServer(port);
        server.initialize();
		server.execute();
	}
}