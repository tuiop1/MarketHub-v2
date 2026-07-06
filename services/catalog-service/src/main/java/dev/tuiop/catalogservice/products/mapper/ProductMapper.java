package dev.tuiop.catalogservice.products.mapper;


import dev.tuiop.catalogservice.products.Product;
import dev.tuiop.catalogservice.products.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    ProductResponse toResponse(Product product);





}
