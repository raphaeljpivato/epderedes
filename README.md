### Exercício Programa de Redes de Computadores
### Projeto de simulação de email com protocolos UDP e TPC:

A nova estrutura de classes está organizada em pacotes. 

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
