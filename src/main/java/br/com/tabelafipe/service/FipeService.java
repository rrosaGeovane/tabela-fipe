package br.com.tabelafipe.service;

import br.com.tabelafipe.dto.Brand;
import br.com.tabelafipe.dto.Model;
import br.com.tabelafipe.dto.Vehicle;
import br.com.tabelafipe.dto.Year;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;


@Service
public class FipeService {

    private final RestClient client =
            RestClient.create("https://fipe.parallelum.com.br/api/v2");

    // Find the brands
    public List<Brand> findBrands(String vehicleType) {
        return client.get()
                .uri("/{type}/brands", vehicleType)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Brand>>() {});
    }

    // Find the models
    public List<Model> findModels(String vehicleType, String brandCode) {
        return client.get()
                .uri("/{type}/brands/{brand}/models", vehicleType, brandCode)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Model>>() {});
    }

    // Find the years
    public List<Year> findYears(String vehicleType, String brandCode, String modelCode) {
        return client.get()
                .uri("/{type}/brands/{brand}/models/{model}/years",
                        vehicleType, brandCode, modelCode)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Year>>() {});
    }

    // Find the vehicle
    public Vehicle findVehicle(String vehicleType, String brandCode, String modelCode, String yearCode) {
        return client.get()
                .uri("/{type}/brands/{brand}/models/{model}/years/{year}",
                        vehicleType, brandCode, modelCode, yearCode)
                .retrieve()
                .body(Vehicle.class);
    }
}