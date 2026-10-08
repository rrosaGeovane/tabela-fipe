package br.com.tabelafipe.dto;

public record Veiculo(int vehicleType,
                      String price,
                      String brand,
                      String model,
                      int modelYear,
                      String fuel,
                      String codeFipe,
                      String referenceMonth,
                      String fuelAcronym) {

    @Override
    public String toString() {
        return """
                Vehicle Type: %d
                Price: %s
                Brand: %s
                Model: %s
                Model Year: %d
                Fuel: %s
                Code Fipe: %s
                Reference Month: %s
                Fuel Acronym: %s
                """.formatted(vehicleType,
                price, brand, model, modelYear,
                fuel, codeFipe, referenceMonth ,fuelAcronym);
    }
}
