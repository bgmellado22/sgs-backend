package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.Usuario;
import com.conectatech.sgs_backend.model.enums.RolUsuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByRun(String run);

    List<Usuario> findByRolAndEstado(RolUsuario rol, Boolean estado);
}