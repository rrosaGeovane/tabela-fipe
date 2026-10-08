package br.com.tabelafipe.dto;

public record Ano(String code,
                  String name) {

    @Override
    public String toString() {
        return "%s: %s\n".formatted(name, code);
    }
}
