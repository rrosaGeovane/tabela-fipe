package br.com.tabelafipe.service;

import br.com.tabelafipe.dto.Ano;
import br.com.tabelafipe.dto.Marca;
import br.com.tabelafipe.dto.Modelo;
import br.com.tabelafipe.dto.Veiculo;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;


@Service
public class FipeService {

    private final RestClient client =
            RestClient.create("https://fipe.parallelum.com.br/api/v2");

    //Buscar as Marcas
    public List<Marca> buscarMarcas (){
        return client.get()
                .uri("/cars/brands")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Marca>>() {});
    }

    //Buscar os modelos
    public List<Modelo> buscarModelos (String codigoMarca) {
        return client.get()
                    .uri("/cars/brands/{marca}/models", codigoMarca)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Modelo>>() {});
    }

    //Buscar os anos
    public List<Ano> buscarAno(String codigoMarca, String codigoModelo){
        return client.get()
                .uri("/cars/brands/{marca}/models/{modelo}/years", codigoMarca, codigoModelo)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Ano>>() {});

    }

    //buscarVeiculo
    public Veiculo buscarVeiculo(String codigoMarca, String codigoModelo, String codigoAno) {
        return client.get()
                    .uri("/cars/brands/{marca}/models/{modelo}/years/{ano}/", codigoMarca, codigoModelo, codigoAno)
                    .retrieve()
                    .body(Veiculo.class);

    }
}
