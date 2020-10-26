package server;

import java.io.*;
import java.text.SimpleDateFormat;
import java.net.*;
import java.util.*;
import client.EMailMessage;

/**
 * Classe responsavel por recepcionar os dados enviados pelo usuario, por processa-los e entao enviar-lhe
 * 	uma resposta a depender do comando. 
 * E aberta uma instancia desta classe para cada novo usuario que faz login no sistema, assim fixando uma
 * 	arquitetura multi-thread que permite que diversos clientes se comuniquem com o servidor simultaneamente
 * Como atributos, possui:
 * 	Socket socket - instancia do socket de conexao entre clientes e servidor
 * 	EMailServer server - instancia que referencia a efetiva classe do servidor
 * 	BufferedReader reader - stream de alto nivel para recepcionar dados em formato de texto (String) enviados
 * 		pelos clientes
 * 	PrintWriter writer - stream de alto nivel para enviar dados em formato de texto (String) aos clientes
 * 	ObjectInputStream objectInFromClient - stream de entrada de dados conectada a entrada generica que 
 * 		remonta objetos de e-mail para sua manipulacao dentro da classe
 * 	InputStream inFromClient - stream generica que se conecta ao socket para recepcionar os dados enviados
 * 		pelos clientes
 * 	OutputStream outToServer - stream generica que se conecta ao socket para despachar os dados aos clientes
 * 	ObjectOutputStream objectOutToClient - stream de saida que monta objetos a serem trafegados por meio da
 * 		stream generica de saida	
 */
public class UserThread extends Thread {
	private Socket socket;
	private EMailServer server;
	private BufferedReader reader;
	private PrintWriter writer;
	private DataOutputStream dataWriter;
	private ObjectInputStream objectInFromClient;
	private InputStream inFromClient;
	private OutputStream outToClient;
	private ObjectOutputStream objectOutToClient;

	/**
	 * Construtor recebe o socket criado para comunicacao com os clientes e a instancia que referencia 
	 * propriamente o servidor. Por meio do socket sao instanciadas as streams de entrada e saida genericas
	 * e, a partir destas, as de alto nivel e de objetos (tambem para entrada e para saida de dados)
	 */
	public UserThread(Socket socket, EMailServer server) {
		this.socket = socket;
		this.server = server;

		try {
			inFromClient = socket.getInputStream();
			outToClient = socket.getOutputStream();
			reader = new BufferedReader( new InputStreamReader( inFromClient ));
			writer = new PrintWriter(outToClient, true);
			dataWriter = new DataOutputStream(outToClient);
			objectInFromClient = new ObjectInputStream(inFromClient);
			objectOutToClient = new ObjectOutputStream(outToClient);
		} 
		catch (IOException ex) {
			System.out.println("Error getting stream on UserThread construction: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * Logica principal da thread de um usuario:
	 * Visto que a primeira acao de um novo usuario conectado e a realizacao do seu login (informar seu 
	 * 	e-mail ao servidor) - a primeira acao da thread deste usuario e recepcionar esse e-mail a fim de 
	 * 	informa-lo ao servidor para que este mantenha controle dos usuarios conectados, bem como e criado
	 * 	um diretorio especifico para esse usuario na particao raiz se usuarios no servidor.
	 * Subsequentemente e iniciado o laco que recepciona os dados enviados pelo usuario e os processa
	 * 	de acordo com o comando executado, assim e recepcionado um dado de texto (clientMessage) que e entao
	 * 	validado. 
	 * 	Como ha a validacao da sintaxe na thread de escrita - responsavel por transmitir os dados do usuario
	 * 	para esta thread - aqui consta a outra parte desse contrato, identificando qual foi o comando enviado
	 * 	Caso seja /sendMessage, significa que o usuario em breve enviara uma mensagem em formato de objeto 
	 * 	de e-mail, desta forma, esta thread fica esperando por dados desse tipo para recepciona-los 
	 * 	adequadamente e assim gravar o e-mail em seu respectivo diretorio no servidor. Como, resposta, 
	 * 	envia novamente o comando /sendMessage para que a thread de leitura do usuario se prepare para 
	 * 	recepcionar a resposta da execucao do procedimento pelo servidor.
	 * 	Caso o comando seja /receiveMessages e entao iniciado o loop de leitura e transmissao das mensagens 
	 * 	recebidas (e seus anexos, caso existam) pelo cliente requisitante. Com sucesso, todos as mensagens
	 * 	sao baixadas na maquina do cliente. Caso haja erro, esse e transmitido e impresso no terminal do
	 * 	cliente.
	 * 	Caso o comando seja /logoff, este mesmo comando e retransmitido para que a thread de leitura possa
	 * 	ter seu encerramento limpo. Na sequencia o usuario e removido da lista de e-mails logados controlada
	 * 	pelo servidor e o socket com esse cliente e encerrado, resultando tambem no termino desta thread.
	 */
	public void run() {
		try {
			String userEMail = reader.readLine();
			server.addUserEMail(userEMail);
			server.makeUserDirectory(userEMail);

			String clientMessage;

			do {
				clientMessage = reader.readLine();

				if (clientMessage.equals("/sendMessage")){
					try {
						receiveMessage(userEMail);
						// writer.println("/sendMessage");
						// writer.println("E-mail sent to server succesfully!");
						dataWriter.writeUTF("/sendMessage");
						dataWriter.writeUTF("E-mail sent to server succesfully!");
					}
					catch (ClassNotFoundException ex) {
						// writer.println("/sendMessage");
						// writer.println("Object class not found: " + ex.getMessage());
						dataWriter.writeUTF("/sendMessage");
						dataWriter.writeUTF("Object class not found: " + ex.getMessage());
					}
					catch (IOException ex) {
						// writer.println("/sendMessage");
						// writer.println("Error while saving the e-mail: " + ex.getMessage());
						dataWriter.writeUTF("/sendMessage");
						dataWriter.writeUTF("Error while saving the e-mail: " + ex.getMessage());
					}
				}	

				else if (clientMessage.equals("/receiveMessages")) {
					try {
						fetchMessages(userEMail, clientMessage);
					}
					catch (IOException ex) {
						// writer.println("Error while downloading your e-mails: " + ex.getMessage());
						dataWriter.writeUTF("Error while downloading your e-mails: " + ex.getMessage());
					}
				}

				else
					// writer.println("/logoff");
					dataWriter.writeUTF("/logoff");

			} while (!clientMessage.equals("/logoff"));

			server.removeUser(userEMail, this);
			socket.close();

		} catch (IOException ex) {
			System.out.println("Error in UserThread: " + ex.getMessage());
		}
	}
	
	/**
	 * Responsavel por implementar o comportamento de receber uma mensagem de e-mail enviada por um usuario
	 * e armazena-la corretamente no servidor.
	 * Recebe por parametro o usuario de e-mail que se comunica com esta thread.
	 * Permanece aguardando o envio de um objeto de e-mail, sendo este desserializado e remontado quando 
	 * enfim transmitido.
	 * Obtem-se, a partir do objeto, a lista de destinatarios deste e-mail.
	 * Para evitar duplicidade no armazenamento dos e-mails e anexos, e adicionado um sufixo em formato de 
	 * Timestamp ao nome do arquivo, produzindo a seguinte sintaxe:
	 * 	Mensagens e e-mail: received-timestamp.txt ou sent-timestamp.txt
	 * 	Anexos: nomeDoArquivo-timestamp.extensao
	 * 	Assim, não há sobrescrita caso dois usuarios distintos possuam um arquivo chamado, por exemplo,
	 * 	"foto3x4.jpg", pois serao compreendidos com o acrescimo da timestamp que torna a probabilidade 
	 * 	de repeticao praticamente nula.
	 * Portanto, essa timestamp e obtida via chamada a classe Date e entao formatada pela SimpleDateFormat.
	 * Como multiplos destinatarios podem ser especificados, e feito um laco por cada destinatario em que 
	 * e verificado se este ja possui diretorio proprio no sistema de arquivos do servidor - caso nao exista
	 * e entao criado - e em seguida a mensagem e gravada com a sintaxe received-timestamp.txt
	 * Terminando a operacao de salvar mensagens, e entao verificado se possuia anexo no e-mail. Em caso
	 * positivo, e entao salvo no diretorio de arquivos conforme sintaxe supracitada.
	 * Por fim, o servidor grava uma copia da mensagem no diretorio do usuario que enviou a mensagem, com a 
	 * sintaxe sent-timestamp.txt
	 */
	void receiveMessage(String userEmail) throws IOException, ClassNotFoundException {
		EMailMessage eMailMessage = (EMailMessage) objectInFromClient.readObject();

		String[] addresses = eMailMessage.getAddressee().split(";");

		Date currentDate = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("-yyyyMMdd-HHmmss");
		String formattedDate = simpleDateFormat.format(currentDate);

		for (int i = 0 ; i < addresses.length ;i++){
			server.makeUserDirectory(addresses[i]);
			saveMessage( addresses[i], buildMessageBuffer(eMailMessage), true, userEmail, formattedDate );
		}

		if(eMailMessage.hasAttachment())
			saveAttachment(eMailMessage.getAttachmentName(), eMailMessage.getAttachmentContent(), formattedDate);

		saveMessage( userEmail, buildMessageBuffer(eMailMessage), false, userEmail, formattedDate );
	}
	
	/**
	 * Responsavel por efetivamente salvar uma mensagem de e-mail no diretorio especifico do usuario. 
	 * Recebe por parametro o e-mail do usuario que identifica o diretorio em que a mensagem deve ser salva,
	 * o buffer com cada linha do e-mail ja traduzida para String, uma flag que identifica se este usuario
	 * deve ser tratado como destinatario da mensagem (negativo significa que e o remetente), o e-mail do
	 * remetente da mensagem e a timestamp ja formatada para coposicao do nome do arquivo.
	 * Uma stream de escrita e inicializada. 
	 * Em seguida e formado o path em que a mensagem sera salva - caso seja um destinatario a sintaxe 
	 * contera received, caso contrario sent.
	 * Caso o usuario de e-mail seja diferente do remetente, e entao atribuido o remetente da mensagem ao
	 * primeiro elemento do buffer (isso se deve ao fato de o primeiro atributo de um e-mail ser o 
	 * destinatario da mensagem, contudo, da perspectiva do destinatario, nao tem muita serventia ter 
	 * conhecimento de que era o destinatario de alguma mensagem, mas, sim, saber quem lhe enviou essa 
	 * mensagem)
	 * Em seguida, caso haja anexo no e-mail, e realizada uma correcao no quinto atributo (indice 4) 
	 * do e-mail/buffer. Para o servidor, nao importa o path em que o anexo estava na maquina do cliente, 
	 * portanto, pelo path e obtido o nome do arquivo e, em sequencia, seu prefixo e substituido para o
	 * do diretorio raiz de arquivos do servidor - isso facilita a recuperacao desse arquivo posteriormente.
	 * Por fim, a mensagem e gravada em formato .txt
	 */
	private void saveMessage(String userEMail, String[] buffer, boolean isAddressee, String sender, String formattedDate){
		BufferedWriter fileStream = null;
		String fileName;

		if ( isAddressee ) 
			fileName = server.getUserDirectoryPath() + "\\" + userEMail + "\\received" + formattedDate + ".txt";
		else
			fileName = server.getUserDirectoryPath() + "\\" + userEMail + "\\sent" + formattedDate + ".txt";

		if (!userEMail.equals(sender))
			buffer[0] = sender;

		if (Boolean.parseBoolean(buffer[3])) {	
			String[] filePath = buffer[4].split("\\\\");
			filePath = formatFilePath(filePath[filePath.length - 1]);			
			buffer[4] = server.getFileDirectoryPath() + "\\" + filePath[0] + formattedDate + "." + filePath[1];
		}

		try {
			fileStream = new BufferedWriter ( new FileWriter ( fileName ));
			fileStream.write(buffer[0], 0, buffer[0].length());

			for (int i = 1; i < buffer.length; i++) {
				fileStream.newLine(); 
				fileStream.write(buffer[i], 0, buffer[i].length());
			}

			fileStream.close();
		} 
		catch (IOException e) {
			System.out.println("Error recording the file: " + fileName + "\n" + e.toString());
			return;
		}
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
	 * Recebe uma stream de arquivo de uma mensagem de e-mail, le cada linha e retorna um array de String
	 * (buffer) contendo cada atributo do objeto de e-mail decodificado.
	 */
	private String[] buildMessageBuffer(String fileName) {
		BufferedReader fileReader = null;
		String[] buffer = new String[5];

		try {
			fileReader = new BufferedReader( new FileReader(fileName) );
			String linha = fileReader.readLine();

			for (int i = 0; linha != null; i++) {
				buffer[i] = linha;
				linha = fileReader.readLine();
			}

			fileReader.close();
		}
		catch (IOException exception) {
			writer.println("Error reading the file: " + fileName + "\n" + exception.toString());
		}

		if (buffer[4] == null)
			buffer[4] = "";
		
		return buffer; 
	}

	/**
	 * Responsavel por salvar um anexo de uma mensagem de e-mail no diretorio raiz de arquivos do servidor.
	 * Recebe por parametro o nome do anexo (conforme e salvo na propria mensagem de e-mail), o buffer de
	 * bytes com o conteudo do anexo em si e a timestamp formatada.
	 * E inicializada uma stream de gravacao.
	 * O path e tratado para que haja a seguinte adequacao:
	 * 	Tomando como exemplo C:\dir1\dir2\nomeDoArquivo.ext que suspostamente foi enviado por um cliente
	 * 	generico, o servidor deve armazena-lo em C:\JavaEMailServer\Files\nomeDoArquivo-timestamp.ext
	 * 	Assim, e obtido o ultimo termo do path original, seu valor nominal e sua extensao sao separados e
	 * 	e formada uma String iniciando pelo diretorio raiz de arquivos do servidor acrescido do valor 
	 * 	nominal acrescido da timestamp acrescido da extensao.
	 * Por fim, a stream de gravacao e chamada concluindo a operacao.
	 */
	private void saveAttachment(String attachmentName, byte[] buffer, String formattedDate) {
		BufferedOutputStream fileStream = null;
		String[] filePath = attachmentName.split("\\\\");
		filePath = formatFilePath(filePath[filePath.length - 1]);
		String fileName = server.getFileDirectoryPath() + "\\" + filePath[0] + formattedDate + "." + filePath[1];

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
	 * Recebe o nome do anexo como parametro, proveniente do objeto de e-mail. Esse nome e entao formatado
	 * e retornado como um vetor de Strings com dois elementos, sendo o primeiro o nome e o segundo a extensao
	 * do arquivo.
	 */
	private String[] formatFilePath(String attachmentName) {
		String reversePath = new StringBuilder(attachmentName).reverse().toString(); 
		
		char[] reversePathChars = reversePath.toCharArray();
		char[] reverseExtensionChars = new char[reversePathChars.length];
		char[] reverseNameChars = new char[reversePathChars.length];
		boolean dotFound = false;
		int itName = 0;
		
		for (int i = 0; i < reversePathChars.length; i++) {
			if (reversePathChars[i] != '.' && !dotFound) 
				reverseExtensionChars[i] = reversePathChars[i];
			
			else if (!dotFound) 
				dotFound = true;

			else {
				reverseNameChars[itName] = reversePathChars[i];
				itName++;
			}
		}
		
		String reverseExtension = new String(reverseExtensionChars).trim();
		String reverseName = new String(reverseNameChars).trim();
		
		String[] response = new String[2];
		response[0] = new StringBuilder(reverseName).reverse().toString();
		response[1] = new StringBuilder(reverseExtension).reverse().toString();
		
		return response;
	}

	/**
	 * Responsavel por ler todas as mensagens recebidas de um determinado cliente de e-mail e transmiti-las
	 * para que sejam baixadas em sua maquina local (anexos inclusos).
	 * Recebe por parametro o e-mail do usuario que se comunica com esta thread e sua ultima mensagem enviada.
	 * 	(/receiveMessages)
	 * O path do diretorio do usuario e montado e entao seus arquivos (mensagens de e-mail) sao listados.
	 * Em sequencia um buffer de leitura desses arquivos e declarado em forma de ArrayList, sendo adicionados
	 * a esse buffer apenas os e-mails recebidos (com prefixo received).
	 * Entao um laco que percorrera cada arquivo desse buffer e montado, operando sob o seguinte contrato:
	 * 	Uma transmissão de objetos de e-mail sera iniciada, mas, para isso, a thread de leitura que 
	 * 	recepcionara esses objetos deve compreender quando aguardar pelo recebimento de um objeto e quando
	 * 	essa transmissao finda. Dessa forma, esta thread envia de modo intercalado uma replica do comando
	 * 	/receiveMessages - para sinalizar que o comando permanece ativo - seguida de um objeto de e-mail.
	 * 	Para finalizar o loop e enviado o comando /stopMessageReceiving
	 * Portanto, o laco consiste em enviar a mensagem sinalizando a transmissao, o objeto de e-mail e 
	 * entao montado a partir da leitura do arquivo armazenado do diretorio do servidor. Caso possua anexo
	 * este tem seus bytes extraidos para serem enviados pelo proprio objeto.
	 */
	public void fetchMessages(String userEmail, String clientMessage) throws IOException {
		String directoryPath = server.getUserDirectoryPath() + "\\" + userEmail;
		File userDirectory = new File(directoryPath);
		String[] filesList = userDirectory.list();
		List<String> fetchBuffer = new ArrayList<String>();
		
		for (String file : filesList) {    
            if (file.startsWith("received"))
            	fetchBuffer.add(file);
        }
		
		for (int i = 0; i < fetchBuffer.size(); i++) { 
			// writer.println(clientMessage);
			dataWriter.writeUTF(clientMessage);

			EMailMessage emailMessage = new EMailMessage();
			
			String[] messageBuffer = buildMessageBuffer( directoryPath + "\\" + fetchBuffer.get(i) );
			
			emailMessage.setAddressee(messageBuffer[0]);
			emailMessage.setSubject(messageBuffer[1]);
			emailMessage.setBody(messageBuffer[2]);
			emailMessage.setAttachment( Boolean.parseBoolean(messageBuffer[3]) );
			emailMessage.setAttachmentName(messageBuffer[4]);
			
			if ( emailMessage.hasAttachment() ) {
				FileInputStream FIS = new FileInputStream( emailMessage.getAttachmentName() );
				emailMessage.setAttachmentContent( FIS.readAllBytes() );
				FIS.close();
			}
			
			objectOutToClient.writeObject(emailMessage);
			objectOutToClient.flush();

			/* File file = new File(directoryPath + "\\" + fetchBuffer.get(i));
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			emailMessage.setAddressee(br.readLine());
			emailMessage.setSubject(br.readLine());
			emailMessage.setBody(br.readLine());
			
			String hasAttachment = br.readLine();
			
			if (Boolean.parseBoolean(hasAttachment)) {
				emailMessage.setAttachment(true);
				emailMessage.setAttachmentName(br.readLine());
				FileInputStream FIS = new FileInputStream(emailMessage.getAttachmentName());
				emailMessage.setAttachmentContent(FIS.readAllBytes());
				FIS.close();
			}
			else {
				emailMessage.setAttachment(false);
				emailMessage.setAttachmentName("");
				emailMessage.setAttachmentContent(null);
			}
			
			objectOutToClient.writeObject(emailMessage);
			objectOutToClient.flush();
			br.close();	 */
        } 		
		// writer.println("/stopMessageReceiving");
		dataWriter.writeUTF("/stopMessageReceiving");
	}
}