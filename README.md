# Bistro Restaurant Management System

Bistro is a client-server restaurant management system developed in Java and JavaFX.  
The system supports restaurant reservations, subscriber management, waiting lists, payments, table management, and administrative tools through dedicated client and server applications.

This repository contains a cleaned and organized version of the academic project, prepared for portfolio presentation.

## Features

### Customer & Subscriber
- Create restaurant reservations
- View existing reservations
- Join and manage the waiting list
- Subscriber login and account management
- QR-based subscriber identification
- View account and visit history
- Complete reservation payments

### Restaurant Staff
- Register and manage subscribers
- View active reservations
- View current customers
- Manage waiting lists
- Access reservation information

### Management
- Manage restaurant tables
- Configure opening hours
- View reports and restaurant statistics
- Access management information
- Dedicated manager and representative interfaces

## Technologies

- Java
- JavaFX
- FXML
- MySQL
- JDBC
- OCSF client-server framework
- Eclipse
- ZXing for QR functionality
- Jakarta Mail

## Architecture

The application follows a client-server architecture:

```text
JavaFX Client
     |
     | OCSF
     v
Java Server
     |
     | JDBC
     v
MySQL Database