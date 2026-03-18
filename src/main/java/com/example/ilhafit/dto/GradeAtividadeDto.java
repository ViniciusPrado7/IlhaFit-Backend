package com.example.ilhafit.dto;
 
import lombok.Data;
import java.util.List;
 
@Data
public class GradeAtividadeDTO {
    private String atividade;
    private Boolean exclusivoMulheres;
    private List<String> diasSemana;
    private List<String> períodos;
}