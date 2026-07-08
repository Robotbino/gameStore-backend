# gameStore-backend

REST API for a game store application built with Spring Boot. Supports user registration and login with JWT authentication, role-based access (USER / ADMIN), and CRUD management of games and users, backed by a MySQL database.

## Tech Stack

- **Java 17**
- **Spring Boot 3.3.4** (Web, Data JPA, Security)
- **MySQL** with Hibernate (`ddl-auto=update`)
- **JWT** authentication via jjwt 0.11.5
- **Lombok** for boilerplate reduction
- **Maven** build

## Features Implemented So Far

### Authentication & Security
- User registration and login endpoints that return a signed JWT (24-hour expiration) with the user's role as a claim.
- Stateless session management — every request is authenticated by the `JWTAuthenticationFilter` reading the `Authorization: Bearer` header.
- Passwords are hashed with Spring Security's `PasswordEncoder` before being stored.
- Automatic role assignment on registration: the account whose email matches the `admin.email` property becomes `ADMIN`; everyone else gets `USER`.
- Duplicate email registration is rejected.
- JWT secret and expiration are configurable through `application.properties` / environment.

### Games
- Full CRUD: create, list all, find by id, update, and delete games.
- Game entity includes title, genre, price, rating, description, and image URLs (card + hero image).

### Users
- Full CRUD: create, list all, update, and delete users.
- `Users` implements Spring Security's `UserDetails` — authentication is done by email.
- Users track loyalty `points` and an `enabled` flag.

### Purchases
- `Purchases` entity models the many-to-many relationship between users and games (one user → many purchases, one game → many purchases) with a purchase timestamp. Endpoints for purchases are not built yet.

### CORS
- Configured for a frontend running at `http://localhost:5173` (Vite dev server), with credentials allowed.

## API Endpoints

### Auth — `/api/v2/auth` (public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/auth/register` | Register a new user, returns a JWT |
| POST | `/api/v2/auth/authenticate` | Log in with email + password, returns a JWT |

### Games — `/games`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/games/add` | Add a new game |
| GET | `/games/all` | Get all games |
| GET | `/games/find/{id}` | Get a game by id |
| PUT | `/games/{id}` | Update a game |
| DELETE | `/games/{id}` | Delete a game |

### Users — `/users`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/users/add` | Add a new user |
| GET | `/users/all` | Get all users |
| PUT | `/users/{id}` | Update a user |
| DELETE | `/users/{id}` | Delete a user |

> **Note:** `/games/**` and `/users/**` are currently `permitAll` in the security config for development. Locking these down by role is on the roadmap.

## Getting Started

### Prerequisites
- Java 17+
- MySQL running locally
- Maven (or use the included `mvnw` wrapper)

### Setup

1. Create the database:
   ```sql
   CREATE DATABASE gamestore_db;
   ```
   (Hibernate creates/updates the tables automatically on startup.)

2. Configure `src/main/resources/application.properties` with your own values:
   - `spring.datasource.username` / `spring.datasource.password` — your MySQL credentials
   - `jwt.secret` — generate your own key, e.g. `openssl rand -base64 32`
   - `admin.email` — the email that should receive the ADMIN role on registration

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

The API starts on **http://localhost:8181**.

### Example: register and authenticate

```bash
# Register
curl -X POST http://localhost:8181/api/v2/auth/register \
  -H "Content-Type: application/json" \
  -d '{"userName": "bino", "email": "bino@example.com", "password": "secret"}'

# Log in
curl -X POST http://localhost:8181/api/v2/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email": "bino@example.com", "password": "secret"}'
```

Both return `{"token": "<jwt>"}` — send it on protected requests as `Authorization: Bearer <jwt>`.

## Project Structure

```
src/main/java/com/gameStore/Bino/
├── authentication/     # Register/login request & response DTOs
├── configuration/      # Security filter chain, JWT filter, CORS, app beans
├── controllers/        # Auth, Games, and Users REST controllers
├── models/             # Games, Users, Purchases entities + Role enum
├── repositories/       # Spring Data JPA repositories
└── service/            # Auth, JWT, Games, and Users business logic
```

## Roadmap

- [ ] Purchase endpoints (buy a game, list a user's purchases)
- [ ] Role-based restrictions on games/users endpoints (e.g. ADMIN-only game management)
- [ ] Validation and proper error responses (replace `RuntimeException`)
- [ ] Tests
