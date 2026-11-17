package co.com.crediya.api.revision;

import co.com.crediya.model.pagina.Pagina;
import co.com.crediya.model.solicitudrevision.SolicitudRevision;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PaginaResponse(
        @JsonProperty("contenido") List<SolicitudRevisionResponse> contenido,
        @JsonProperty("pagina") int pagina,
        @JsonProperty("tamano") int tamano,
        @JsonProperty("total_elementos") long totalElementos,
        @JsonProperty("total_paginas") int totalPaginas) {

    public static PaginaResponse from(Pagina<SolicitudRevision> pagina) {
        return new PaginaResponse(
                pagina.contenido().stream().map(SolicitudRevisionResponse::from).toList(),
                pagina.pagina(),
                pagina.tamano(),
                pagina.totalElementos(),
                pagina.totalPaginas());
    }
}
