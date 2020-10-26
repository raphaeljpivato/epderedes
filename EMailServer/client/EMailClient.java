package client;

import java.net.*;
import java.io.*;

/**
 * Classe responsavel por instanciar um cliente de e-mail.
 * Nela sao definidos os diretorios raizes de cliente de e-mail utilizados pelo sistema, bem como sao 
 * mantidos o nome do hostname e a porta do servidor com quem este cliente se conecta e o nome de seu respectivo
 * usuario.
 * A classe opera orquestrando duas threads, uma de leitura e uma de escrita, garantindo mais responsividade e 
 * melhorando a organizacao nas trocas de dados com o servidor. A thread de leitura le os dados enviados do 
 * servidor para o cliente e a de escrita obtem os dados por parte do cliente para envia-los ao servidor.
 */
public class EMailClient {
	private String hostname;
	private int port;
	private String userEmail;
	private final String serverRootPath = "C:\\JavaEMailClient";
	private final String userDirectoryPath = serverRootPath + "\\Users";
	private final String fileDirectoryPath = serverRootPath + "\\Files";

	public EMailClient(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}

	public String getUserEmail() {
		return this.userEmail;
	}

	public String getUserDirectoryPath() {
		return this.userDirectoryPath;
	}
	
	public String getFileDirectoryPath() {
		return this.fileDirectoryPath;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	/**
	 * Inicializa a estrutura de diretorios do cliente para recepcionar os e-mails e arquivos enviados pelo 
	 * servidor, sendo criados caso nao existam ainda ou realizando bypass para prevenir erros em tempo de 
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
	 * Viabiliza a criacao de um diretorio especifico para o usuario de e-mail passado por parametro, util 
	 * quando mais de um usuario de e-mail opera como cliente em uma mesma maquina local.
	 * Tal qual no metodo de inicializacao, firma o contrato de criar tal diretorio somente caso ele nao 
	 * exista previamente.
	 */
	public void makeUserDirectory(String userEmail) {
		File userEmailDirectory = new File(userDirectoryPath + "\\" + userEmail);

		if (!userEmailDirectory.exists())
			userEmailDirectory.mkdirs();
	}

	/**
	 * Realiza a conexao com o socket do servidor, exibe as opcoes disponiveis para o usuario e instancia as
	 * threads de leitura e escrita.
	 */
	public void execute() {
		try {
			Socket socket = new Socket(hostname, port);

			System.out.println("Connected to the e-mail server\n");

			showOptions();

			new WriteThread(socket, this).start();
			new ReadThread(socket, this).start();
			
		} catch (UnknownHostException ex) {
			System.out.println("Server not found: " + ex.getMessage());
		} catch (IOException ex) {
			System.out.println("I/O Error: " + ex.getMessage());
		}
	}
	
	/**
	 * Exibe as opcoes do usuario para que este interaja com o sistema.
	 */
	void showOptions() {
		System.out.println("*** ------------------- Welcome to our E-Mail Server! --------------------- ***");
		System.out.println("*** First you need to login using your e-mail address                       ***");
		System.out.println("*** Then you can use the commands listed below:                             ***");
		System.out.println("*** \t/sendMessage     - you'll be able to type a text message.           ***");
		System.out.println("*** \t/receiveMessages - you'll be able to download all your e-mails.     ***");
		System.out.println("*** \t/logoff          - you'll log out and terminate the client program. ***");
		System.out.println("*** ----------------------------------------------------------------------- ***");
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Syntax: java EMailClient <hostname> <port-number>");
			System.exit(0);
		}

		String hostname = args[0];
		int port = Integer.parseInt(args[1]);

		EMailClient client = new EMailClient(hostname, port);
		client.initialize();
		client.execute();
	}
}