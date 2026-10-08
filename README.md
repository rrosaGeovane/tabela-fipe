# 🚗 FIPE Table API Consumer

A Spring Boot command-line application that consumes the public **FIPE Table API** to retrieve the average market price of vehicles in Brazil and display the results directly in the terminal.

This project was built as a hands-on study of **how to consume an external REST API with Spring**, instead of building one. Most tutorials teach how to *create* APIs; this one focuses on the other side of the conversation: being the **client**.

---

## 📌 About the FIPE Table

The FIPE Table (*Tabela FIPE*) is the reference for average vehicle prices in the Brazilian market. It is widely used for buying and selling cars, insurance, and financing. The data is updated monthly.

This project uses the community-maintained API available at [fipe.api.br](https://fipe.api.br), which exposes the FIPE data as JSON over REST.

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Java 17+** | Language |
| **Spring Boot 3.2+** | Application framework |
| **Spring Web** | Provides `RestClient` (HTTP calls) and Jackson (JSON mapping) |
| **RestClient** | Sends HTTP requests to the external API |
| **Jackson** | Converts JSON responses into Java objects |
| **Java Records** | Immutable data models (DTOs) |
| **Maven** | Build and dependency management |

---

## ⚙️ How It Works

The application starts, makes an HTTP `GET` request to the FIPE API, converts the JSON response into a Java object, and prints the vehicle details in the terminal.

```
Spring Boot starts
      │
      ▼
CommandLineRunner.run() is triggered
      │
      ▼
RestClient sends GET request ──────► FIPE API
                                        │
                                        ▼
                                  Looks up its own database
                                        │
      ┌─────────────────────────────────┘
      ▼
JSON response arrives
      │
      ▼
Jackson deserializes JSON ──► Veiculo (record)
      │
      ▼
Result printed in the terminal
```

### 🏪 Mental model

To understand each component, I used a store analogy:

| In the analogy | In the code |
|---|---|
| Supplier with the stock | FIPE API and its database |
| Shipping company | `RestClient` |
| Employee who unpacks the boxes | Jackson |
| Shelf format | Records (`Veiculo`) |
| Opening routine of the store | `CommandLineRunner` |
| Store door closed to the public | No embedded web server (`web-application-type=none`) |
| Rules for writing the orders | REST (HTTP methods + URLs) |

---

## 📂 Project Structure

```
src/main/java/br/com/tabelafipe/
├── TabelaFipeApplication.java   # Entry point (@SpringBootApplication)
├── FipeRunner.java              # Makes the request and prints the result
└── Veiculo.java                 # Record that maps the JSON response

src/main/resources/
└── application.properties       # Disables the web server
```

---

## 🔍 Code Walkthrough

### 1. Running as a terminal application

Spring Web includes an embedded Tomcat server. Since this project only needs to make requests (not receive them), the server is disabled:

```properties
spring.main.web-application-type=none
```

### 2. The data model

The API returns a single JSON object with the vehicle details:

```json
{
  "brand": "...",
  "codeFipe": "...",
  "fuel": "...",
  "fuelAcronym": "...",
  "model": "...",
  "modelYear": 2020,
  "price": "R$ ...",
  "referenceMonth": "...",
  "vehicleType": 1
}
```

A Java **record** maps these fields. The field names must match the JSON keys exactly so Jackson can fill them automatically:

```java
public record Veiculo(int vehicleType,
                      String price,
                      String brand,
                      String model,
                      int modelYear,
                      String fuel,
                      String codeFipe,
                      String referenceMonth,
                      String fuelAcronym) { }
```

> `price` is a `String`, not a `double`, because the API returns it as formatted text (`"R$ 10.000,00"`).
> Fields present in the JSON but missing from the record (such as `priceHistory`) are simply ignored.

### 3. Making the request

```java
@Component
public class FipeRunner implements CommandLineRunner {

    private final RestClient client =
            RestClient.create("https://fipe.parallelum.com.br/api/v2");

    @Override
    public void run(String... args) throws Exception {

        Veiculo veiculo = client
                .get()
                .uri("/cars/brands/25/models/7693/years/2020-5")
                .retrieve()
                .body(Veiculo.class);

        System.out.println(veiculo.brand() + " - " + veiculo.model());
        System.out.println("Year: " + veiculo.modelYear());
        System.out.println("Price: " + veiculo.price());
    }
}
```

| Step | What it does |
|---|---|
| `@Component` | Spring creates this object automatically |
| `CommandLineRunner` | Spring calls `run()` as soon as the application starts |
| `RestClient.create(...)` | Static factory method that builds the client with the base URL |
| `.get()` | Defines the HTTP method (fetch data) |
| `.uri(...)` | Appends the endpoint path to the base URL |
| `.retrieve()` | Sends the request and receives the response |
| `.body(Veiculo.class)` | Jackson converts the JSON into a `Veiculo` object |

---

## 🌐 API Endpoints

Base URL: `https://fipe.parallelum.com.br/api/v2`

The API works like a funnel, where each request narrows down the search:

| Step | Endpoint | Returns |
|---|---|---|
| 1 | `/{vehicleType}/brands` | List of brands |
| 2 | `/{vehicleType}/brands/{brandId}/models` | List of models |
| 3 | `/{vehicleType}/brands/{brandId}/models/{modelId}/years` | List of years |
| 4 | `/{vehicleType}/brands/{brandId}/models/{modelId}/years/{yearId}` | Vehicle details and price |

`vehicleType` can be `cars`, `motorcycles`, or `trucks`.

**Rate limit:** 500 requests per day without authentication, or 1,000 per day with a free token sent in the `X-Subscription-Token` header.

---

## ▶️ How to Run

**Requirements:** Java 17+ and Maven (or use the included Maven Wrapper).

```bash
# Clone the repository
git clone https://github.com/<your-username>/tabela-fipe.git
cd tabela-fipe

# Run
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

---

## 📚 What I Learned

- **API vs. database:** an API is the communication layer; the data lives in a database behind it. As a client, I never access the database directly.
- **Being a client, not a server:** how to make an application *consume* an API instead of *exposing* one.
- **Spring Boot starters:** what `spring-boot-starter-web` actually brings (Spring MVC, Tomcat, Jackson, RestClient) and which parts this project uses.
- **`CommandLineRunner`:** runs code once at startup, similar to a `main` method, but with Spring managing object creation.
- **`RestClient`:** building HTTP requests with method chaining (`get → uri → retrieve → body`).
- **Static factory methods:** why `RestClient.create()` is used instead of `new` (`RestClient` is an interface).
- **Jackson and deserialization:** how JSON keys are mapped to record fields by name, and why data types must match (a formatted price is a `String`, not a `double`).
- **Single object vs. list:** `{ }` maps to `.body(MyClass.class)`, while `[ ]` requires `ParameterizedTypeReference<List<MyClass>>` because of Java's **type erasure**.
- **HTTP status codes:** `200` success, `404` not found, `429` too many requests, `500` server error, and how `retrieve()` throws exceptions on errors.
- **Exception handling:** the difference between `throws` (passing the error up) and `try/catch` (handling it).
- **Git and GitHub:** initializing a repository, committing, and pushing a project.

---

## 🚀 Next Steps

- [ ] Make it interactive: let the user choose brand, model, and year in the terminal
- [ ] Handle errors with `try/catch` (invalid codes, rate limit exceeded)
- [ ] Support motorcycles and trucks
- [ ] Send the access token through the `X-Subscription-Token` header
- [ ] Display the price history
- [ ] Expose the data through my own REST endpoints

---

## 👤 Author

**Geovane Ribeiro Rosa**
Information Systems student at Uniube (Universidade de Uberaba)

---

## 📄 Credits

Vehicle data provided by [FIPE API](https://fipe.api.br), a free community API by Deivid Fortuna.
