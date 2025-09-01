# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java web application called "Elephant" - a timesheet management system with geospatial features. The application uses modern Java 21 features including virtual threads and preview features.

## Core Architecture

### Technology Stack
- **Language**: Java 21 (with preview features enabled)
- **Web Server**: Eclipse Jetty 12.0.9 with HTTP/1.1, HTTP/2, and HTTP/3 support
- **Database**: PostgreSQL with Hibernate ORM 6.5.2.Final and spatial extensions
- **Authentication**: Kerberos/SPNEGO with LDAP integration
- **Frontend**: Custom Elements with OpenLayers for mapping, Server-Sent Events for real-time updates
- **Build Tool**: Maven

### Key Components

- **SrvApp**: Main application entry point that configures Jetty server, database connections, LDAP, and Kerberos authentication
- **Controllers**: 
  - `TscController`: Handles timesheet operations with state machine (LOADING → EDITING → SUBMITTED → CLOSED)
  - `GenericController`: Base controller with common functionality
- **Entities**: JPA entities in `net.coplanar.ents` package for database persistence
- **Real-time Communication**: Server-Sent Events via `SseHandler` for live updates
- **Authentication**: `KerberosHandler` for SPNEGO authentication with LDAP user lookup

### Database Schema
Uses Hibernate with automatic schema updates (configured as "update" - potentially dangerous in production). Key entities include:
- Project management (Project, ProjectSite, ProjectState)
- User management (TsUser) 
- Timesheet cells (TsCell)
- Geographic data with PostGIS spatial types

## Development Commands

### Build and Run
```bash
# Compile the project
mvn compile

# Run the application
mvn exec:java

# Package with dependencies
mvn package
```

### Testing
```bash
# Run tests
mvn test
```

### Development Configuration

The application expects these configuration files in the root directory:
- `config.properties`: Kerberos keytab path and other settings
- `logging.properties`: Java logging configuration
- `tls/`: Directory containing SSL certificates and keystores

### Database Connection
Uses Unix domain socket connection to PostgreSQL:
```
jdbc:postgresql:///ts?socketFactory=org.newsclub.net.unix.AFUNIXSocketFactory$FactoryArg&socketFactoryArg=/var/run/postgresql/.s.PGSQL.5432
```

### Development Notes

- Virtual threads are enabled via QueuedThreadPool configuration
- The application supports client certificate authentication
- HTTP/3 support requires PEM certificates in the `tls/` directory
- Schema generation is set to "update" mode - be cautious in production
- LDAP connection uses connection pooling
- The application integrates with systemd for service management

### Frontend Architecture
- Custom Elements for UI components (`ts-cell`, `ts-row`, `proj-map`)
- OpenLayers for mapping functionality
- Server-Sent Events for real-time updates between client and server
- Jackson for JSON serialization/deserialization

### Security Considerations
- Kerberos/SPNEGO authentication with keytab files
- LDAP integration for user directory lookups
- SSL/TLS with client certificate support
- Trust store configuration for certificate validation