package br.com.tabelafipe;

import br.com.tabelafipe.dto.Ano;
import br.com.tabelafipe.dto.Marca;
import br.com.tabelafipe.dto.Modelo;
import br.com.tabelafipe.dto.Veiculo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Scanner;

@Component
public class FipeRunner implements CommandLineRunner {

    Scanner sc = new Scanner(System.in);

    private final RestClient client =
            RestClient.create("https://fipe.parallelum.com.br/api/v2");

    @Override
    public void run(String... args) throws Exception {

        String codigoMarca;
        String codigoModelo;
        String codigoAno;

        //Mostra as Marcas
        List<Marca> marcas = client
                .get()
                .uri("/cars/brands")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Marca>>() {});

        marcas.forEach(m -> System.out.println(m.code() + " - " + m.name()));


        //Seleciona a marca e mostra os modelos
        while (true){
            System.out.println("Digite o código da Marca: ");
            codigoMarca = sc.nextLine().trim();
            System.out.println("Você digitou: [" + codigoMarca + "]");

            try {
                List<Modelo> modelos = client
                        .get()
                        .uri("/cars/brands/{marca}/models", codigoMarca)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<Modelo>>() {
                        });

                System.out.println("Quantidade de modelos: " + modelos.size());
                modelos.forEach(m -> System.out.println(m.code() + " - " + m.name()));
                break;

            } catch (HttpClientErrorException.NotFound e){
                System.out.println("Não existe essa Marca. TENTE NOVAMENTE!");
            }
        }

        //Seleciona o modelo e mostra os anos
        while (true){
            System.out.println("Digite o modelo: ");
            codigoModelo = sc.nextLine().trim();
            System.out.println("Você digitou: [" + codigoModelo + "]");

            try {
                List<Ano> ano = client
                        .get()
                        .uri("/cars/brands/{marca}/models/{modelo}/years", codigoMarca, codigoModelo)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<Ano>>() {
                        });

                ano.forEach(m -> System.out.println(m.code() + " - " + m.name()));
                break;

            } catch (HttpClientErrorException.NotFound e){
                System.out.println("Não existe esse modelo. TENTE NOVAMENTE!");
            }
        }

        //seleciona o ano e mostra o veículo
        while (true){
            System.out.println("Digite o ano: ");
            codigoAno = sc.nextLine().trim();
            System.out.println("Você digitou: [" + codigoAno + "]");

            try {
                Veiculo veiculo = client
                        .get()
                        .uri("/cars/brands/{marca}/models/{modelo}/years/{ano}/", codigoMarca, codigoModelo, codigoAno)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Veiculo>() {
                        });

                System.out.println("\n===== RESULTADO =====");
                System.out.println("Veículo: " + veiculo.brand() + " - " + veiculo.model());
                System.out.println("Ano: " + veiculo.modelYear());
                System.out.println("Combustível: " + veiculo.fuel());
                System.out.println("Código FIPE: " + veiculo.codeFipe());
                System.out.println("Preço: " + veiculo.price());
                System.out.println("Referência: " + veiculo.referenceMonth());
                break;

            } catch (HttpClientErrorException.NotFound e){
                System.out.println("Não existe esse ano. TENTE NOVAMENTE!");
            }
        }
    }
}
