package dev.tuiop.paymentservice.payments.mapper;

import dev.tuiop.paymentservice.payments.Payment;
import dev.tuiop.paymentservice.payments.dto.PaymentResultResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "info", source = "failureReason")
    PaymentResultResponse toResultResponse(Payment payment);
}
