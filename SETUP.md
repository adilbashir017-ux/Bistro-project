# Bistro Local Setup

This document explains how to configure and run the Bistro Restaurant Management System locally.

## Requirements

Install the following software:

- JDK 25
- JavaFX SDK 25
- MySQL Server 8
- Eclipse IDE

## Import the Eclipse Projects

Import the following three projects into the same Eclipse workspace:

```text
ClientBistro
ServerBistro
OCSF
```

The client and server depend on the local `OCSF` project.

## Eclipse User Libraries

Create the following Eclipse User Libraries.

### JAVAFX

Add all JavaFX JAR files from the `lib` directory of your JavaFX SDK installation.

Example:

```text
javafx-sdk-25/lib
```

### BISTRO_LIBS

Add the following dependencies:

- ZXing Core 3.5.2
- ZXing JavaSE 3.5.2
- Jakarta Mail 2.0.1
- Jakarta Activation 2.0.1
- MySQL Connector/J 9.5.0

The `ClientBistro` and `ServerBistro` projects reference these Eclipse User Libraries.

## JavaFX VM Arguments

For both the client and server run configurations, configure the JavaFX module path.

Example:

```text
--module-path "PATH_TO_JAVAFX\lib" --add-modules javafx.controls,javafx.fxml
```

Replace `PATH_TO_JAVAFX` with the location of your JavaFX SDK.

## Database Setup

Import:

```text
database/BistroDatabase.sql
```

into MySQL.

The application uses the Bistro MySQL database through JDBC.

## Environment Variables

The server requires:

```text
BISTRO_DB_USER
BISTRO_DB_PASSWORD
```

Optional database configuration:

```text
BISTRO_DB_URL
```

Optional email configuration:

```text
BISTRO_EMAIL
BISTRO_EMAIL_APP_PASSWORD
```

If email credentials are not configured, the system continues to operate without sending email notifications.

The client can optionally use:

```text
BISTRO_SERVER_HOST
BISTRO_SERVER_PORT
```

By default, the application connects to:

```text
localhost:5555
```

See `.env.example` for the available configuration variables.

## Run the Application

Start the applications in the following order:

1. Run `Server.ServerUI` from `ServerBistro`.
2. Enter port:

```text
5555
```

3. Confirm that the server is listening.
4. Run `client.ClientUI` from `ClientBistro`.

The client should then connect to the local server.

## Security

Do not store database passwords, email application passwords, or other credentials directly in the source code.

Use environment variables for local configuration.