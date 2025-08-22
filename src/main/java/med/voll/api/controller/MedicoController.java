package med.voll.api.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import med.voll.api.FileStorage.FileStorageProperties;
import med.voll.api.FileStorage.FileStorageService;
import med.voll.api.endereco.Endereco;
import med.voll.api.infra.LeitorCSV;
import med.voll.api.medico.DadosAtualizacaoMedico;
import med.voll.api.medico.DadosCadastroMedico;
import med.voll.api.medico.DadosDetalhamentoMedico;
import med.voll.api.medico.DadosListagemMedico;
import med.voll.api.medico.Especialidade;
import med.voll.api.medico.Medico;
import med.voll.api.medico.MedicoRepository;

@RestController
@RequestMapping("/medicos")
public class MedicoController {

    @GetMapping
    public ResponseEntity<Page<DadosListagemMedico>> listar(
            @PageableDefault(size = 10, sort = { "nome" }) Pageable paginacao) {
        var page = repository.findAllByAtivoTrue(paginacao).map(DadosListagemMedico::new);

        return ResponseEntity.ok(page);
    }

    @PutMapping
    @Transactional
    public ResponseEntity atualizar(@RequestBody @Valid DadosAtualizacaoMedico dados) {
        var medico = repository.getReferenceById(dados.id());
        medico.atualizarInformacoes(dados);

        return ResponseEntity.ok(new DadosDetalhamentoMedico(medico));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        var medico = repository.getReferenceById(id);
        medico.excluir();

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DadosDetalhamentoMedico> detalhar(@PathVariable Long id) {
        var medico = repository.getReferenceById(id);

        return ResponseEntity.ok(new DadosDetalhamentoMedico(medico));
    }

    private final LeitorCSV leitorCSV;
    private final Path fileStorageLocation;

    public MedicoController(LeitorCSV leitorCSV, FileStorageProperties fileStorageProperties) {
        this.leitorCSV = leitorCSV;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath()
                .normalize();
    }

    @Autowired
    private MedicoRepository repository;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<Medico> cadastrar(
            @RequestPart("dados") DadosCadastroMedico dados,
            @RequestPart(value = "arquivo", required = false) MultipartFile arquivo) {

        Endereco endereco = new Endereco(
                dados.endereco().logradouro(),
                dados.endereco().bairro(),
                dados.endereco().cep(),
                dados.endereco().complemento(),
                dados.endereco().numero(),
                dados.endereco().uf(),
                dados.endereco().cidade());

        // Criar o objeto Medico
        Medico medico = new Medico();
        medico.setNome(dados.nome());
        medico.setEmail(dados.email());
        medico.setCrm(dados.crm());
        medico.setEspecialidade(dados.especialidade());
        medico.setTelefone(dados.telefone());
        medico.setEndereco(endereco); // Atribui o objeto Endereco

        if (arquivo != null && !arquivo.isEmpty()) {
            String fileName = fileStorageService.storeFile(arquivo);
            medico.setFoto(fileName);
        }

        repository.save(medico);
        return ResponseEntity.ok(medico);
    }

    @GetMapping(value = "/{id}/download", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> downloadArquivo(@PathVariable Long id) throws IOException {
        Optional<Medico> optionalMedico = repository.findById(id);
        if (optionalMedico.isEmpty() || optionalMedico.get().getFoto() == null) {
            return ResponseEntity.notFound().build();
        }

        String fileName = optionalMedico.get().getFoto();
        Path imagePath = Paths.get("./vollmed-files/" + fileName);

        if (Files.exists(imagePath)) {
            byte[] imageBytes = Files.readAllBytes(imagePath);
            return ResponseEntity.ok().body(imageBytes);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles() throws IOException {
        List<String> fileNames = Files.list(fileStorageLocation)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileNames);
    }

    @PostMapping("/importar")
    public ResponseEntity<String> importar(@RequestParam("file") MultipartFile file) {
        try {
            leitorCSV.importar(file);
            return ResponseEntity.ok("Importação realizada com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao importar: " + e.getMessage());
        }
    }
}

