package co.com.crediya.r2dbc.usuario;

import co.com.crediya.model.exceptions.BusinessErrorType;
import co.com.crediya.model.exceptions.BusinessException;
import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.model.usuario.gateways.UsuarioRepository;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public class UsuarioRepositoryAdapter extends ReactiveAdapterOperations<
        Usuario,
        UsuarioEntity,
        Long,
        UsuarioReactiveRepository
        > implements UsuarioRepository {

    public UsuarioRepositoryAdapter(UsuarioReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, entity -> mapper.map(entity, Usuario.class));
    }

    @Override
    @Transactional
    public Mono<Usuario> save(Usuario usuario) {
        return super.save(usuario)
                .doOnSubscribe(s -> log.debug("Guardando usuario en la base de datos"))
                .doOnSuccess(guardado -> log.info("Usuario guardado con id {}", guardado.getIdUsuario()))
                .onErrorMap(UsuarioRepositoryAdapter::traducirError)
                .doOnError(e -> log.warn("No se pudo guardar el usuario: {}", e.getMessage()));
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Mono<Boolean> existsByDocumentoIdentidad(String documentoIdentidad) {
        return repository.existsByDocumentoIdentidad(documentoIdentidad);
    }

    @Override
    public Mono<Usuario> findByDocumentoIdentidad(String documentoIdentidad) {
        return repository.findByDocumentoIdentidad(documentoIdentidad).map(this::toEntity);
    }

    @Override
    public Mono<Usuario> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toEntity);
    }

    private static Throwable traducirError(Throwable error) {
        if (error instanceof DuplicateKeyException) {
            return new BusinessException(BusinessErrorType.ALREADY_EXISTS,
                    "El correo o el documento ya están registrados");
        }
        if (error instanceof DataIntegrityViolationException) {
            return new BusinessException(BusinessErrorType.INVALID_DATA,
                    "Los datos del usuario no cumplen las reglas de la base de datos");
        }
        return error;
    }
}
