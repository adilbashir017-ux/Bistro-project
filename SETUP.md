# Bistro local setup notes

This cleaned source snapshot removes generated binaries, nested Git metadata, machine-specific paths, and hard-coded credentials.

## Required software
- Java JDK compatible with your JavaFX SDK
- JavaFX SDK
- MySQL
- Eclipse (optional)

## Eclipse user libraries
Create these Eclipse User Libraries:

### JAVAFX
Add the JavaFX SDK JAR files from your local JavaFX installation.

### BISTRO_LIBS
Add:
- ZXing `core` 3.5.2
- ZXing `javase` 3.5.2
- Jakarta Mail 2.0.1
- Jakarta Activation 2.0.1
- MySQL Connector/J 9.5.0

The ClientBistro and ServerBistro `.classpath` files reference those libraries and the local OCSF project.

## Configuration
Set the environment variables listed in `.env.example`.

At minimum, the server requires `BISTRO_DB_USER` and `BISTRO_DB_PASSWORD`.
Email notifications additionally require `BISTRO_EMAIL` and `BISTRO_EMAIL_APP_PASSWORD`.
The client defaults to `localhost:5555`; override with `BISTRO_SERVER_HOST` and `BISTRO_SERVER_PORT`.

## Database
Import `database/BistroDatabase.sql` into MySQL. The included seed data is sanitized for public sharing.

## Run
1. Start `ServerBistro` using `Server.ServerUI`.
2. Start `ClientBistro` using `client.ClientUI`.
