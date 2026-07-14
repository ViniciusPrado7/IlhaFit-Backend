package com.example.ilhafit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailDTO {

    @NotBlank(message = "Destinatário é obrigatório")
    @Email(message = "Destinatário deve ser um email válido")
    private String to;

    @NotBlank(message = "Assunto é obrigatório")
    @Size(max = 120, message = "Assunto deve ter no máximo 120 caracteres")
    private String subject;

    @NotBlank(message = "Mensagem é obrigatória")
    @Size(max = 5000, message = "Mensagem deve ter no máximo 5000 caracteres")
    private String message;

    public EmailDTO() {
    }

    public EmailDTO(String to, String subject, String message) {
        this.to = to;
        this.subject = subject;
        this.message = message;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
