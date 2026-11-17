# Smart Clinic Management System

A clinic portal where **admins** manage doctors, **patients** find doctors and book
appointments, and **doctors** see their schedule and write prescriptions. Built with
Spring Boot, MySQL, MongoDB and plain HTML/CSS/JavaScript, as the capstone of the IBM
Java Development with Databases course.

## Architecture

Three tiers:

| Layer | Technology | Where |
|---|---|---|
| Presentation | Thymeleaf dashboards (admin, doctor) and static HTML/JS pages (patient), calling REST APIs | `app/src/main/resources/templates`, `.../static` |
| Application | Spring Boot controllers → services → repositories | `app/src/main/java/com/project/back_end` |
| Data | MySQL for admins, doctors, patients and appointments; MongoDB for prescriptions | see [schema-design.md](schema-design.md) |

Requests flow from the browser to a controller, which checks the JWT for the caller's role,
calls a service for the business rules (free slots, ownership, duplicate checks), and the
service reads and writes through Spring Data repositories.

## Documents

- [User stories](user_stories.md), also tracked as [GitHub Issues](https://github.com/n-sharma23/java-database-capstone/issues)
- [Schema design](schema-design.md)
- [Stored procedures](app/src/main/resources/db/stored_procedures.sql)
- [Screenshots](screenshots/) and [SQL/API output](docs/submission-outputs/)

## Run locally

Requirements: JDK 17+, MySQL 8, MongoDB 6+.

```bash
cd app
export JWT_SECRET="$(openssl rand -base64 48)"   # required: signs login tokens
export MYSQL_PASSWORD=...                          # if your MySQL root user has a password
export SEED_DEMO_DATA=true                         # optional: demo doctors, patients, appointments
./mvnw spring-boot:run
```

Open <http://localhost:8080>. Then load the reporting procedures once:

```bash
mysql -u root cms < app/src/main/resources/db/stored_procedures.sql
```

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | none (required) | HMAC key for JWTs, at least 32 characters |
| `MYSQL_HOST` / `MYSQL_PORT` | `localhost` / `3306` | MySQL server; database `cms` is created if missing |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `root` / empty | MySQL credentials |
| `MONGODB_URI` | `mongodb://localhost:27017/prescriptions` | Prescription store |
| `SEED_DEMO_DATA` | `false` | Insert demo data into empty tables |

**Demo accounts** (only created with `SEED_DEMO_DATA=true`; local use only):

| Role | Login | Password |
|---|---|---|
| Admin | `admin` | `admin@1234` |
| Doctor | `emily.adams@clinic.example` | `doctor@123` |
| Patient | `john.smith@mail.example` | `patient@123` |

### Docker

```bash
docker build -t smart-clinic .
docker run -p 8080:8080 -e JWT_SECRET=... -e MYSQL_HOST=host.docker.internal \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/prescriptions smart-clinic
```

## Tests and CI

```bash
cd app && ./mvnw test
```

GitHub Actions ([compile-backend.yml](.github/workflows/compile-backend.yml)) compiles the
backend and runs the unit tests on every push and pull request to `main`.

## Design notes

- **Passwords** are stored as BCrypt hashes and never returned by the API.
- **Tokens** carry the user's email and role. A token is only accepted for its own role,
  and patients can only see or change their own appointments; doctors only see their own.
- **No double booking:** a booking must match one of the doctor's free slots for that day.
- **Known trade-off:** the course's frontend passes the JWT as a URL path segment
  (for example `/appointments/{token}`). URLs end up in server and proxy logs, so a production
  version should send it in an `Authorization: Bearer` header instead.

## API overview

| Method | Path | Who |
|---|---|---|
| POST | `/admin` | Admin login |
| GET | `/doctor` | Anyone: all doctors |
| GET | `/doctor/filter/{name}/{time}/{speciality}` | Anyone: filter (`null` = any) |
| POST / PUT | `/doctor/{token}` | Admin: add / update doctor |
| DELETE | `/doctor/{id}/{token}` | Admin: delete doctor and their appointments |
| POST | `/doctor/login` | Doctor login |
| GET | `/doctor/availability/{user}/{doctorId}/{date}/{token}` | Free slots on a date |
| POST | `/patient` | Patient sign-up |
| POST | `/patient/login` | Patient login |
| GET | `/patient/{token}` | Patient: own profile |
| GET | `/patient/{id}/{user}/{token}` | Patient: own appointments; doctor: appointments with them |
| GET | `/patient/filter/{condition}/{name}/{token}` | Patient: filter by past/future and doctor |
| GET | `/appointments/{date}/{patientName}/{token}` | Doctor: appointments on a date |
| POST / PUT | `/appointments/{token}` | Patient: book / reschedule |
| DELETE | `/appointments/{id}/{token}` | Patient: cancel |
| POST | `/prescription/{token}` | Doctor: add prescription (marks appointment completed) |
| GET | `/prescription/{appointmentId}/{token}` | Doctor: view prescription |
