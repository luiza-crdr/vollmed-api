package med.voll.api.infra;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import med.voll.api.endereco.Endereco;
import med.voll.api.medico.Especialidade;
import med.voll.api.medico.Medico;
import med.voll.api.medico.MedicoRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class LeitorCSV {

    private final MedicoRepository medicoRepository;

    // Injeção pelo construtor (boa prática com Spring)
    public LeitorCSV(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    public void importar(MultipartFile file) throws Exception {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String linha;
            boolean primeiraLinha = true;

            while ((linha = br.readLine()) != null) {
                if (primeiraLinha) { 
                    primeiraLinha = false; // pula o cabeçalho
                    continue;
                }

                String[] campos = linha.split(";");

                Medico medico = new Medico();
                medico.setNome(campos[0]);
                medico.setEmail(campos[1]);
                medico.setCrm(campos[2]);

                // ENUM especialidade
                medico.setEspecialidade(Especialidade.valueOf(campos[3].toUpperCase()));

                medico.setTelefone(campos[4]);

                // ENDEREÇO (classe embutida)
                Endereco endereco = new Endereco(
                        campos[5],  // logradouro
                        campos[6],  // bairro
                        campos[7],  // cep
                        campos[8],  // complemento
                        campos[9],  // numero
                        campos[10], // uf
                        campos[11]  // cidade
                );
                medico.setEndereco(endereco);

                medicoRepository.save(medico);
            }
        }
    }
}
