package client;

/**
 * Esta classe e responsavel por simular um interpretador bastante simples de comandos de entrada do
 * usuario, tendo como principal comportamento determinar os comandos aceitos pelo sistema.
 */

public class CommandReader {
    
    private String userInput;

    public CommandReader() {
        this.userInput = "";
    }

    public String getUserInput() {
        return this.userInput;
    }

    private void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    /**
     * Verifica se o comando passado por parametro esta entre um dos aceitos, retornando true; ou false caso
     * possua sintaxe diferente
     */
    public boolean isACommand(String command) {
        if (command.equals("/sendMessage") || command.equals("/receiveMessages") || command.equals("/logoff"))
            return true;
        else 
            return false;
    }

    /**
     * Le o comando de entrada passado por parametro, armazena-o via setter para que possa ser resgatado 
     * futuramente e checa se e efetivamente um comando valido para o sistema. Este metodo retorna Strings
     * que em caso negativo podem ser impressas no terminal para informar o usuario de que a sintaxe foi 
     * invalida.
     */
    public String readCommand(String command) {      
        String response = "Ok";
        setUserInput(command);

        if (!isACommand(command))
            response = "Invalid command, please enter a new one";

        return response;
    }
}