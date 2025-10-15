# Stock Analyzer BTST Analytics

This project provides an automated workflow for analysing "Buy Today Sell Tomorrow" (BTST) opportunities.

## Database Configuration

- By default (the `default`/`dev` profile) the application uses an in-memory [H2](https://www.h2database.com/) database. The configuration is defined in `src/main/resources/application.yml` and connects to `jdbc:h2:mem:testdb`, so no standalone database server is required for local development. Data is reset whenever the application restarts because it is kept purely in memory.
- For production-like usage you can enable the `prod` profile, which expects a MySQL-compatible database. A reference configuration is provided in `src/main/resources/application-prod.yml`, and the `docker/docker-compose.yml` file can be used to run MySQL locally if desired.

Choose the profile that matches your scenario. If you only need the in-memory database, nothing else needs to be provisioned.
