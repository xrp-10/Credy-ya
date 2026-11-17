package co.com.crediya.model.pagina;

import java.util.List;

/** Una página de resultados con lo necesario para pedir la siguiente. */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <T> Pagina<T> de(List<T> contenido, int pagina, int tamano, long totalElementos) {
        int totalPaginas = (int) ((totalElementos + tamano - 1) / tamano);
        return new Pagina<>(List.copyOf(contenido), pagina, tamano, totalElementos, totalPaginas);
    }
}
