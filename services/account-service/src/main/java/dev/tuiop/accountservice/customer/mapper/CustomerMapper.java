package dev.tuiop.accountservice.customer.mapper;

import dev.tuiop.accountservice.customer.Customer;
import dev.tuiop.accountservice.customer.dto.CustomerRegistrationRequest;
import dev.tuiop.accountservice.customer.dto.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AddressMapper.class)
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", source = "keycloakUserId")
    @Mapping(target = "firstName", source = "request.firstName")
    @Mapping(target = "lastName", source = "request.lastName")
    @Mapping(target = "birthDate", source = "request.birthDate")
    @Mapping(target = "email", expression = "java(request.email().trim())")
    @Mapping(target = "adress", source = "request.address")
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(String keycloakUserId, CustomerRegistrationRequest request);

    @Mapping(target = "address", source = "adress")
    CustomerResponse toResponse(Customer customer);
}
