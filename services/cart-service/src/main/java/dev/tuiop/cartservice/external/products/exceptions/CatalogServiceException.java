package dev.tuiop.cartservice.external.products.exceptions;

import dev.tuiop.cartservice.common.exceptions.BusinessException;

public class CatalogServiceException extends BusinessException {

    private CatalogServiceException(String code, String message, int status, Throwable cause) {
        super(code, message, status, cause);
    }

    public static CatalogServiceException unavailable(Throwable cause) {
        return new CatalogServiceException(
                "CATALOG_SERVICE_UNAVAILABLE",
                "Catalog service is currently unavailable",
                503,
                cause
        );
    }

    public static CatalogServiceException unauthorized(Throwable cause) {
        return new CatalogServiceException(
                "CATALOG_SERVICE_AUTHORIZATION_FAILED",
                "Cart service is not authorized to call catalog service",
                503,
                cause
        );
    }

    public static CatalogServiceException rejected(Throwable cause) {
        return new CatalogServiceException(
                "CATALOG_SERVICE_REJECTED_REQUEST",
                "Catalog service rejected the purchase request",
                409,
                cause
        );
    }
}
