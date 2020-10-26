package client;

import java.io.Serializable;

/**
 * Classe entidade, responsavel por estabelecer os atributos de um objeto de mensagem de e-mail, sendo eles:
 *  addressee - destinatario da mensagem (multiplos destinatarios sao tratados como enderecos de e-mail separados
 *   por ';') 
 *  subject - assunto do e-mail
 *  body - corpo do e-mail
 *  attachment - booleana que determina se este objeto tambem carrega um anexo ou nao 
 *  attachmentName - nome do anexo sendo o path para instanciacao das streams de leitura do arquivo anexo
 *  attachmentContent - buffer de bytes que comporta o conteudo propriamente dito do anexo, caso exista
 * Os comportamentos definidos pela classe se restringem a getters e setters dos atributos
 */
public class EMailMessage implements Serializable {
    private String addressee;
    private String subject;
    private String body;
    private boolean attachment;
    private String attachmentName;
    private byte[] attachmentContent;

    public EMailMessage() { 
        this.addressee = "";
        this.subject = "";
        this.body = "";
        this.attachment = false;
        this.attachmentName = "";
        this.attachmentContent = null;
    }
    
    public String getAddressee() {
        return this.addressee;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getBody() {
        return this.body;
    }

    public boolean hasAttachment() {
        return this.attachment;
    }

    public String getAttachmentName() {
        return this.attachmentName;
    }

    public byte[] getAttachmentContent() {
        return this.attachmentContent;
    }

    public void setAddressee(String addressee) {
        this.addressee = addressee;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setAttachment(boolean attachment) {
        this.attachment = attachment;
    }

    public void setAttachmentName(String name) {
        this.attachmentName = name;
    }

    public void setAttachmentContent(byte[] content) { 
        this.attachmentContent = content;
    }
}