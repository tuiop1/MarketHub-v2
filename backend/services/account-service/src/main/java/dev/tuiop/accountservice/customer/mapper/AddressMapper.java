package dev.tuiop.accountservice.customer.mapper;

import dev.tuiop.accountservice.customer.Address;
import dev.tuiop.accountservice.customer.dto.AddressRequest;
import dev.tuiop.accountservice.customer.dto.AddressResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    Address toEntity(AddressRequest request);

    AddressResponse toResponse(Address address);
}
