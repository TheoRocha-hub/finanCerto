package br.com.financerto.repository;

import br.com.financerto.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByUsuarioIdOrderByNome(Long usuarioId);
    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);
    Optional<Categoria> findByUsuarioIdAndNomeIgnoreCase(Long usuarioId, String nome);
}
