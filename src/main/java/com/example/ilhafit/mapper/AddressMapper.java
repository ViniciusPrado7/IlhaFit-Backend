package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.AddressDTO;
import com.example.ilhafit.entity.Address;
import com.example.ilhafit.util.StringNormalizer;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public Address toEntity(AddressDTO dto) {
        if (dto == null)
            return null;
        Address endereco = new Address();
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

    public AddressDTO toDTO(Address entity) {
        if (entity == null)
            return null;
        AddressDTO dto = new AddressDTO();
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
