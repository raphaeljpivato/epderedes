package client;

import java.io.*;
import java.net.*;
// import java.util.Scanner;

/**
 * Classe responsavel por estabelecer o tratamento de dados que o usuario cliente envia para servidor
 * Como atributos, possui:
 * 	OutputStream outToServer - stream generica de saida que se conecta ao socket para despachar os dados
 * 		para o servidor
 * 	ObjectOutputStream objectOutToServer - stream de saida que monta objetos a serem trafegados por meio da
 * 		stream generica de saida	
 * 	PrintWriter writer - stream de escrita que envia os dados em formato de texto (String)
 * 	Socket socket - instancia do socket de conexao entre o cliente e o servidor
 * 	EMailClient client - instancia que referencia a sua propria classe de cliente
 * 	CommandReader userCommand - instancia do interpretador de comandos (ver classe CommandReader para uma
 * 		descricao mais detalhada)
 * 	EMailMessage eMailMessage - objeto que instancia uma mensagem de e-mail (ver classe EMailMessage para uma 
 * 		descricao mais detalhada)
 */
public class WriteThread extends Thread {
	private OutputStream outToServer;
	private ObjectOutputStream objectOutToServer;
	private PrintWriter writer;
	private Socket socket;
	private EMailClient client;
	private CommandReader userCommand;
	private EMailMessage emailMessage;	

	/**
	 * Construtor recebe o socket de conexao e a instancia da classe cliente a que esta thread referencia.
	 * Por meio do socket instancia as streams de transmissao de dados do cliente para o servidor
	 */
	public WriteThread(Socket socket, EMailClient client) {
		this.socket = socket;
		this.client = client;

		try {
			outToServer = socket.getOutputStream();
			writer = new PrintWriter(outToServer, true);
			objectOutToServer = new ObjectOutputStream(outToServer);
		} catch (IOException ex) {
			System.out.println("Error getting output stream: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	/**
	 * Estabelece a interface em estilo de formulario para guiar o usuario no preenchimento do e-mail.
	 * Neste preenchimento o objeto de e-mail é montado e, por fim, enviado ao servidor.
	 * Caso haja anexo no e-mail, uma stream de leitura de arquivos é aberta momentaneamente para 
	 * montar um buffer de bytes sendo este enviado como parte integrante do objeto do e-mail
	 */
	public void sendEmail(Console console) throws IOException {
		emailMessage = new EMailMessage();

		System.out.println("\n*** To send a message to multiple addressees, use ';' as separator");

		emailMessage.setAddressee(console.readLine("\nChoose the addressee(s) which you wish to send an e-mail to: "));
		emailMessage.setSubject(console.readLine("\nPlease type the subject of your e-mail: "));
		emailMessage.setBody(console.readLine("\nPlease type the body of your e-mail: "));
		String attachment = console.readLine("\nWould you like do attach a file to your message? (y/n): ");

		if (attachment.equals("y") || attachment.equals("Y") || attachment.equals("yes") || attachment.equals("YES")) {
			emailMessage.setAttachment(true);
			emailMessage.setAttachmentName(console.readLine("\nPlease enter the file path: "));
			FileInputStream FIS = new FileInputStream(emailMessage.getAttachmentName());
			emailMessage.setAttachmentContent(FIS.readAllBytes());
			FIS.close();
		}
		else {
			emailMessage.setAttachment(false);
			emailMessage.setAttachmentName("");
			emailMessage.setAttachmentContent(null);
		}
		
		System.out.println("\nYour message is being uploaded to the server");

		objectOutToServer.writeObject(emailMessage);
		objectOutToServer.flush();
	}

	/**
	 * Logica principal da thread de escrita:
	 * E instanciado um objeto de Console, o qual e utilizado para recuperacao dos textos digitados pelo 
	 * 	usuario.
	 * E instanciado um objeto de CommandReader - o interpretador de comandos - a fim de validar a sintaxe
	 * 	dos comandos digitados pelo usuario.
	 * O usuario realiza login no sistema digitando seu e-mail (nao e exatamente um login com senha para
	 * 	validacao dos usuarios de e-mail - essa funcionalidade foi simplificada atendo-se somente ao uso
	 * 	dessa String para criacao de um diretorio proprio desse usuario na particao raiz do sistema na 
	 * 	maquina local. Adicionalmente esse e-mail e enviado ao servidor para que este tanto tenha 
	 * 	conhecimento do usuario logado quanto crie um diretorio particular deste tambem)
	 * E entao iniciado o laco desta thread, que basicamente cria um prefixo [user e-mail] e fica aguardando
	 * 	o usuario entrar com algum comando
	 * Esse comando e entao submetido ao interpretador para que este valide sua sintaxe. Caso falhe nesse 
	 * 	teste, uma mensagem de erro devolvida pelo interpretador e impressa na tela do usuario e laco se
	 * 	inicia novamente.
	 * Caso o comando seja valido, este e entao enviado ao servidor - para que o servidor possa se preparar
	 * 	adequadamente para recepcionar os dados a depender do comando.
	 * Para os comandos /logoff e /receiveMessages - estes apenas sao enviados ao servidor. (No caso do 
	 * 	/logoff este provoca o termino da thread do usuario do lado do servidor e o das de escrita e leitura
	 * 	do lado do usuario. No caso do /receiveMessages o servidor se prepara para transmitir os objetos de
	 * 	e-mail recebidos deste determinado usuario)
	 * Para o comando /sendMessage e entao iniciada a sequencia de montagem do objeto de e-mail que o
	 * 	usuario representado por esta thread deseja enviar, concluindo com o seu envio para o servidor.
	 */
	public void run() {

		Console console = System.console();
		userCommand = new CommandReader();

		String userEmail = console.readLine("\nEnter your e-mail login address: ");
		client.setUserEmail(userEmail);
		client.makeUserDirectory(userEmail);
		writer.println(userEmail);

		String text;
		String commandSyntax;

		try {
			do {
				text = console.readLine("[" + userEmail + "]: ");
				commandSyntax = userCommand.readCommand(text);

				if (commandSyntax.equals("Ok")) {

					writer.println(userCommand.getUserInput());
					
					if (userCommand.getUserInput().equals("/sendMessage")) 
						sendEmail(console);
				}
				else
					System.out.println(commandSyntax);
	
			} while (!text.equals("/logoff"));

			System.out.println("Logging out...");
		}
		catch (IOException ex) {
			System.out.println("Error writing to server: " + ex.getMessage());
		}
	}
}