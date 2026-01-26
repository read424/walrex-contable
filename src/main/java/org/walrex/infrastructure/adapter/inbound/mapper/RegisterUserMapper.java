package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.RegisterUserRequest;
import org.walrex.domain.model.Customer;
import org.walrex.domain.model.IdentificationMethod;
import org.walrex.domain.model.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RegisterUserMapper {

    DateTimeFormatter BIRTHDATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Mapping(source = "names", target = "firstName")
    @Mapping(target = "lastName", source = ".", qualifiedByName = "buildLastName")
    @Mapping(target = "birthDate", source = "birthDate", qualifiedByName = "parseBirthDate")
    @Mapping(target = "idProfessional", source = "occupation", qualifiedByName = "parseProfession")
    @Mapping(target = "isPEP", source = "isPoliticallyExposed", qualifiedByName = "mapPep")
    @Mapping(target = "email", source = ".", qualifiedByName = "resolveEmail")
    @Mapping(target = "phoneNumber", source = ".", qualifiedByName = "resolvePhone")
    Customer toCustomer(RegisterUserRequest registerUserRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "usernameType", source = "request.identificationMethod", qualifiedByName = "mapUsernameType")
    @Mapping(target = "pinHash", source = "request.pinHash")
    @Mapping(target = "pinAttempts", constant = "0")
    @Mapping(target = "active", constant = "1")
    User toUser(RegisterUserRequest request, Integer customerId);

    @Named("buildLastName")
    default String buildLastName(RegisterUserRequest req) {
        String first = req.getFirstLastName();
        String second = req.getSecondLastName();

        if (first == null || first.isBlank()) {
            return second != null ? second : "";
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + " " + second;
    }

    @Named("resolveEmail")
    default String resolveEmail(RegisterUserRequest req) {
        return req.getIdentificationMethod() == IdentificationMethod.EMAIL
                ? req.getReferenceId()
                : null;
    }

    @Named("resolvePhone")
    default String resolvePhone(RegisterUserRequest req) {
        return req.getIdentificationMethod() == IdentificationMethod.PHONE
                ? req.getReferenceId()
                : null;
    }

    @Named("parseBirthDate")
    default LocalDate parseBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(birthDate, BIRTHDATE_FORMAT);
    }

    @Named("parseProfession")
    default Integer parseProfession(String occupation) {
        if (occupation == null || occupation.isBlank()) {
            return null;
        }
        return Integer.parseInt(occupation);
    }

    @Named("mapPep")
    default String mapPep(Boolean isPoliticallyExposed) {
        return isPoliticallyExposed != null && isPoliticallyExposed ? "1" : "0";
    }

    @Named("mapUsernameType")
    default String mapUsernameType(IdentificationMethod method) {
        return method != null ? method.name() : null;
    }
}
