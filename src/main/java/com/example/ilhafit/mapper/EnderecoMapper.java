package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.EnderecoDTO;
import com.example.ilhafit.entity.Endereco;
import com.example.ilhafit.util.StringNormalizer;
import org.springframework.stereotype.Component;

@Component
public class EnderecoMapper {

    public Endereco toEntity(EnderecoDTO dto) {
        if (dto == null)
            return null;
        Endereco endereco = new Endereco();
        endereco.setRua(StringNormalizer.normalize(dto.getRua()));
        endereco.setNumero(StringNormalizer.normalize(dto.getNumero()));
        endereco.setComplemento(StringNormalizer.normalize(dto.getComplemento()));
        endereco.setBairro(StringNormalizer.normalize(dto.getBairro()));
        endereco.setCidade(StringNormalizer.normalize(dto.getCidade()));
        endereco.setEstado(dto.getEstado());
        endereco.setCep(dto.getCep());
        endereco.setLatitude(dto.getLatitude());
        endereco.setLongitude(dto.getLongitude());
        return endereco;
    }

    public EnderecoDTO toDTO(Endereco entity) {
        if (entity == null)
            return null;
        EnderecoDTO dto = new EnderecoDTO();
        dto.setRua(entity.getRua());
        dto.setNumero(entity.getNumero());
        dto.setComplemento(entity.getComplemento());
        dto.setBairro(entity.getBairro());
        dto.setCidade(entity.getCidade());
        dto.setEstado(entity.getEstado());
        dto.setCep(entity.getCep());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        return dto;
    }
}