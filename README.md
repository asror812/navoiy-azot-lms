# HR Candidate Testing LMS Backend

Spring Boot backend for HR candidate exam management.

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Security (HTTP Basic for HR)
- Spring Data JPA
- PostgreSQL (dev/prod)
- OpenAPI/Swagger

## Default Credentials

- HR username: `hr`
- HR password: `hr123`

## Environment Variables

`application-dev.yml` and `application-prod.yml` are env-driven.

Required for local PostgreSQL:

- `DB_URL` (default: `jdbc:postgresql://localhost:5432/navoiyazotlms`)
- `DB_USERNAME` (default: `postgres`)
- `DB_PASSWORD` (default: `2702`)

Optional:

- `SERVER_PORT` (default: `8080`)
- `DDL_AUTO` (default: `update`)
- `EXAM_QUESTION_COUNT` (default: `40`)
- `EXAM_DURATION_MINUTES` (default: `60`)
- `EXAM_MAX_ATTEMPTS_PER_CANDIDATE` (default: `0`, unlimited)

## Run Locally

1. Create DB:

```bash
createdb navoiyazotlms
```

2. Run app:

```bash
./gradlew bootRun
```

3. Open API docs:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Main Endpoints

### HR (`/api/hr/*`, requires Basic Auth)

- Jobs: `GET/POST /jobs`, `PUT/DELETE /jobs/{jobId}`
- Questions/Tests: `GET/POST /tests`, `PUT/DELETE /tests/{id}`
- Question direct edit/delete: `PUT/DELETE /questions/{questionId}`
- Candidates: `GET/POST /candidates`, `PUT/DELETE /candidates/{candidateId}`
- Candidate passport update: `PUT /candidates/{candidateId}/passport`
- Results with filters: `GET /results`

### Candidate (`/api/candidate/*`)

- Login (legacy): `POST /auth/login` (`login/password`)
- Login (UI-ready): `POST /auth/passport-login` (`fullName/passport`)
- List random tests by candidate: `GET /{candidateId}/tests`
- Start or resume exam: `POST /tests/start`
- Save progress: `POST /attempts/{attemptId}/progress`
- Get progress: `GET /attempts/{attemptId}/progress?candidateId=...`
- Submit attempt: `POST /attempts/{attemptId}/submit`

## Tests

```bash
./gradlew test
```
