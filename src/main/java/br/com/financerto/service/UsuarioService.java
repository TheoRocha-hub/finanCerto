package br.com.financerto.service;

import br.com.financerto.dto.Dtos.RegistroRequest;
import br.com.financerto.model.Categoria;
import br.com.financerto.model.TipoMovimentacao;
import br.com.financerto.model.Usuario;
import br.com.financerto.repository.CategoriaRepository;
import br.com.financerto.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final PasswordEncoder encoder;

    @Transactional
    public void registrar(RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenha(encoder.encode(req.senha()));
        usuarios.save(u);
        categorias.saveAll(categoriasPadrao(u));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario u = usuarios.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(u.getEmail()).password(u.getSenha()).roles("USER").build();
    }

    @Transactional(readOnly = true)
    public Usuario atual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarios.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private List<Categoria> categoriasPadrao(Usuario u) {
        List<Categoria> lista = new ArrayList<>();
        for (String n : List.of("Salário", "Freelance", "Outras receitas")) {
            lista.add(nova(u, n, TipoMovimentacao.RECEITA));
        }
        for (String n : List.of("Moradia", "Alimentação", "Transporte", "Saúde", "Lazer", "Educação", "Outras despesas")) {
            lista.add(nova(u, n, TipoMovimentacao.DESPESA));
        }
        return lista;
    }

    private Categoria nova(Usuario u, String nome, TipoMovimentacao tipo) {
        Categoria c = new Categoria();
        c.setUsuario(u);
        c.setNome(nome);
        c.setTipo(tipo);
        return c;
    }
}
