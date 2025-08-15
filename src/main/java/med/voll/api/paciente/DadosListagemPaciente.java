package med.voll.api.paciente;

public record DadosListagemPaciente(String nome, String paciente, String cpf) {

    public DadosListagemPaciente(Paciente paciente){
    this(paciente.getNome(), paciente.getEmail(), paciente.getCPF());

    }
}
