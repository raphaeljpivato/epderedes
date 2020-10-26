### Exercício Programa de Redes de Computadores
### Projeto de simulação de email com protocolos UDP e TPC:

Recomendável executar como administrador, visto que há a criação de diretórios e é possível que esbarre em algum problema de autorização que provoque erro não tratado em tempo de execução.

### Para executar os .jar (somente Windows):
- Baixar os arquivos com extensão .jar
- Abrir o cmd (prompt de comando) e mudar para o diretório em que os arquivos .jar foram baixados
- Utilizar o comando abaixo para inicializar o servidor de e-mails:

	``java -jar eMailServer.jar <porta>``
	
	Exemplo: java -jar eMailServer.jar 9090
	
- Abrir novos cmds que atuarão como os clientes
- Nos clientes, executar o comando abaixo para executá-los:

	``java -jar eMailClient.jar <nomeDoHost> <porta>``
	
	Exemplo: java -jar eMailClient.jar localhost 9090

### Para executar via terminal:
A estrutura de classes está organizada em pacotes. Para executar via terminal, seguir as instruções abaixo: 

### Para compilar as classes:
- Abrir um terminal e referenciá-lo para o diretório pai do projeto (um nível acima das pastas client e server)
- Utilizar os comandos:
	
	``javac client/*.java``
	
	``javac server/*.java``

### Para executar:
- Abrir três terminais e referenciá-los para o diretório pai do projeto (um nível acima das pastas client e server).

Um terminal executará a thread do servidor indefinidamente

Os outros dois executarão as threads clientes durante o tempo que o usuário desejar utilizar o serviço

- No terminal que servirá de servidor, executar o comando:
	
	``java server/EMailServer <port>``
	
	Em que <port> é o parâmetro da porta em que o servidor executará

- Em cada terminal em que um cliente será executado, executar o seguinte comando:

	`` java client/EMailClient <hostname> <port> `` 
	
	Em que <hostname> é o parâmetro do nome do host (ou endereço de IP) do servidor.
	
	Em que <port> é o parâmetro da porta em que o servidor executará.
