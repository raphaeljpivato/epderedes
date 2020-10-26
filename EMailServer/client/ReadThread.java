package client;

import java.io.*;
import java.net.*;

/**
 * Classe responsavel por estabelecer o tratamento de dados que o servidor envia para o usuario cliente.
 * Como atributos, possui:
 * 	BufferedReader reader - stream de leitura que recebe dados em formato de texto (String)
 * 	Socket socket - instancia do socket de conexao entre o cliente e o servidor
 * 	EMailClient client - instancia que referencia a sua propria classe de cliente
 * 	EMailMessage eMailMessage - objeto que instancia uma mensagem de e-mail (ver classe EMailMessage para uma 
 * 		descricao mais detalhada)
 * 	InputStream inFromServer - stream generica que se conecta ao socket para obtencao dos dados de entrada
 * 	ObjectInputStream objectInFromServer - stream de entrada que remonta objetos trafegados por meio da
 * 		strema generica de entrada
 */
public class ReadThread extends Thread {
	private BufferedReader reader;
	private DataInputStream dataReader;
	private Socket socket;
	private EMailClient client;
	private EMailMessage eMailMessage;
	private InputStream inFromServer;
	private ObjectInputStream objectInFromServer;

	/**
	 * Construtor recebe o socket de conexao e a instancia da classe cliente a que esta thread referencia.
	 * Por meio do socket instancia as streams de transmissao de dados do servidor para o cliente
	 */
	public ReadThread(Socket socket, EMailClient client) {
		this.socket = socket;
		this.client = client;

		try {
			inFromServer = socket.getInputStream();
			reader = new BufferedReader( new InputStreamReader(inFromServer) );
			dataReader = new DataInputStream(inFromServer);
			objectInFromServer = new ObjectInputStream(inFromServer);
		} 
		catch (IOException ex) {
			System.out.println("Error getting input stream: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * Executa um laco que alterna entre o recebimento de um objeto de e-mail e uma mensagem de texto do servidor.
	 * Esse comportamento foi estabelecido para que a thread interprete o momento de parada dessa transmissao, 
	 * portanto, a cada objeto de e-mail enviado, o servidor envia ou a repeticao do comando /receiveMessages - 
	 * para que a thread permaneca no laco recebendo mais e-mails - ou envia /stopMessageReceiving - provocando 
	 * a parada do laco e consequente termino no download dos e-mails
	 */
	private void downloadMessages(String response) throws IOException {
		while (!response.equals("/stopMessageReceiving")) {
			try {
				eMailMessage = (EMailMessage) objectInFromServer.readObject();

				saveMessage( buildMessageBuffer(eMailMessage) );

				if (eMailMessage.hasAttachment())
					saveAttachment( eMailMessage.getAttachmentName(), eMailMessage.getAttachmentContent() );

				// response = reader.readLine();
				response = dataReader.readUTF();

				if (response.startsWith("Error")) {
					System.out.println(response);
					return;
				}
			}
			catch (ClassNotFoundException exception) {
				System.out.println("Object class not found: " + exception.getMessage());
			}
		}

		System.out.println("E-mail(s) downloaded succesfully!");
	}

	/**
	 * Recebe um objeto de mensagem de e-mail por parametro, le cada atributo seu - com excecao do conteudo do
	 * anexo - e constroi um buffer de Strings em que cada linha representa um atributo.
	 * Indice 0 e o primeiro atributo - addressee (destinatario(s))
	 * Indice 1 e o segundo atributo - subject (assunto)
	 * Indice 2 e o terceiro atributo - body (corpo)
	 * Indice 3 e o quarto atributo - attachment (flag se possui anexo ou nao)
	 * Indice 4 e o quinto atributo - attachmentName (nome do anexo)
	 */
	private String[] buildMessageBuffer(EMailMessage eMailMessage) {
		String[] buffer = new String[5];

		buffer[0] = eMailMessage.getAddressee();
		buffer[1] = eMailMessage.getSubject();
		buffer[2] = eMailMessage.getBody();
		buffer[3] = String.valueOf(eMailMessage.hasAttachment());
		buffer[4] = eMailMessage.getAttachmentName();

		return buffer;
	}

	/**
	 * Recebe o buffer contruido a partir do objeto de e-mail para entao salvar sua mensagem na maquina local
	 * do usuario.
	 * A nomenclatura utilizada foi received-subject, localizado no indice 1 do buffer, simulando o 
	 * comportamento dos serviços de e-mail convencionais.
	 * Para prevenir erros no nome do arquivo em tempo de execucao, e feita substituicao de barras por tracos
	 */
	private void saveMessage(String[] buffer) {
		BufferedWriter fileStream = null;
		String subject = buffer[1].replace("/", "-");
		String fileName = client.getUserDirectoryPath() + "\\" + client.getUserEmail() + "\\received-" + subject + ".txt";

		try {
			fileStream = new BufferedWriter ( new FileWriter ( fileName ));
			// fileStream.write(buffer[0], 0, buffer[0].length());
			fileStream.write(buffer[0]);

			for (int i = 1; i < buffer.length; i++) {
				fileStream.newLine(); 
				// fileStream.write(buffer[i], 0, buffer[i].length());
				fileStream.write(buffer[i]);
			}

			fileStream.close();
		} 
		catch (IOException e) {
			System.out.println("Error recording the file: " + fileName + "\n" + e.toString());
			return;
		}
	}

	/**
	 * Responsavel por salvar um anexo de uma mensagem de e-mail no diretorio raiz de arquivos da maquina do
	 * cliente.
	 * Recebe por parametro o nome do anexo (conforme e salvo na propria mensagem de e-mail), o buffer de
	 * bytes com o conteudo do anexo em si e a timestamp formatada.
	 * E inicializada uma stream de gravacao.
	 * O path e quebrado, sendo o ultimo termo o nome isolado do arquivo junto da extensao.
	 * Por fim, a stream de gravacao e chamada concluindo a operacao.
	 */
	private void saveAttachment(String attachmentName, byte[] buffer) {
		BufferedOutputStream fileStream = null;
		String[] filePath  = attachmentName.split("\\\\");
		String fileName = client.getFileDirectoryPath() + "\\" + filePath[filePath.length - 1];

		try {
			fileStream = new BufferedOutputStream( new FileOutputStream( fileName ));
			fileStream.write(buffer);
			fileStream.close();
		} 
		catch (IOException e) {
			System.out.println("Error recording the file: " + fileName + "\n" + e.toString());
			return;
		}
	}

	/**
	 * Logica principal da thread de leitura:
	 * Cada comando enviado pelo usuario faz com que o servidor retransmita esse comando para que a thread de 
	 * leitura saiba como tratar os dados resultantes desse determinado comando. Assim, a primeira acao e a 
	 * de ler esse comando.
	 * Se esse comando for /sendMessage, significa que o servidor comecou a receber uma mensagem de e-mail.
	 * 	Apos a conclusao dessa operacao (tanto bem sucedida quanto finalizada com erro) o servidor transmite
	 * 	nova mensagem, sendo esta comunicada ao usuario.
	 * Se esse comando for /receiveMessages, significa que o servidor recebeu um pedido de download dos e-mails
	 * por parte do usuario. Dessa forma, o servidor transmitira todos os e-mails recebidos por esse usuario, 
	 * sendo estes gravados em sua maquina local.
	 * Se esse comando for /logoff, significa que as demais threads ja foram encerradas e que esta tambem pode
	 * encerrar, fechando o socket de comunicacao e provocando o termino normal do programa do cliente.
	 */
	public void run() {
		String response = "";

		try {
			do {
				// response = reader.readLine();
				response = dataReader.readUTF();

				if (response.equals("/sendMessage")) {
					// response = reader.readLine();
					response = dataReader.readUTF();
					System.out.println(response);
				}
					
				else if (response.equals("/receiveMessages")) 
					downloadMessages(response);

				else if (response.equals("/stopMessageReceiving"))
					System.out.println("There are no e-mails to download");
					
			} while (!response.equals("/logoff"));
			
			socket.close();
		}
		catch (IOException ex) {
			System.out.println("Error reading from server: " + ex.getMessage());
		}	 
	}
}