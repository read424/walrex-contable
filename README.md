# 📊 Walrex - Sistema de Gestión Contable y Registro de Ventas

![Quarkus](https://img.shields.io/badge/Quarkus-3.30.2-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Reactive-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

## 🎯 Descripción

**Walrex** es un backend robusto y escalable diseñado para la gestión integral de sistemas contables y registro de ventas. Construido con tecnologías modernas y siguiendo las mejores prácticas de arquitectura de software, este sistema proporciona una base sólida para operaciones empresariales críticas.

### Características Principales

- 💼 **Gestión Contable Completa**: Control total de operaciones contables y financieras
- 🛒 **Registro de Ventas**: Sistema eficiente de registro y seguimiento de transacciones de venta
- 👥 **Gestión de Clientes**: Administración completa de información de clientes con validaciones robustas
- 🌍 **Gestión de Catálogos**: Manejo de países, monedas, tipos de documentos SUNAT y del sistema
- 📊 **Reportes y Analytics**: Generación de reportes contables y de ventas en tiempo real
- 🔒 **Seguridad**: Implementación de mejores prácticas de seguridad y validación de datos

## 🏗️ Arquitectura

El sistema está construido siguiendo los principios de **Arquitectura Hexagonal (Ports & Adapters)** y **Clean Architecture**, garantizando:

- **Separación de Responsabilidades**: Cada capa tiene un propósito específico y bien definido
- **Independencia del Framework**: El dominio de negocio no depende de Quarkus
- **Testabilidad**: Arquitectura diseñada para facilitar pruebas unitarias e integración
- **Escalabilidad**: Preparado para crecer según las necesidades del negocio
- **Mantenibilidad**: Código limpio, organizado y fácil de mantener

### Capas de la Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                   │
│  (REST Controllers, Persistence, External Services)     │
├─────────────────────────────────────────────────────────┤
│                   Application Layer                      │
│         (Use Cases, DTOs, Ports Interfaces)             │
├─────────────────────────────────────────────────────────┤
│                     Domain Layer                         │
│        (Business Logic, Domain Models, Rules)           │
└─────────────────────────────────────────────────────────┘
```

## 🛠️ Stack Tecnológico

### Framework Principal
- **Quarkus 3.30.2**: Framework supersónico y subatómico para Java
- **Java 21**: Última versión LTS con las últimas características del lenguaje

### Base de Datos
- **PostgreSQL**: Base de datos relacional robusta
- **Hibernate Reactive Panache**: ORM reactivo para máxima performance
- **Flyway**: Migraciones de base de datos versionadas

### Programación Reactiva
- **Mutiny**: Biblioteca de programación reactiva moderna
- **Vert.x**: Toolkit reactivo de alto rendimiento
- **Reactive PostgreSQL Client**: Cliente PostgreSQL completamente reactivo

### Persistencia y Caché
- **Redis**: Sistema de caché distribuido
- **Redis Cache**: Implementación de caché con Redis

### APIs y Documentación
- **SmallRye OpenAPI**: Generación automática de documentación OpenAPI 3.0
- **Swagger UI**: Interface interactiva para explorar y probar APIs
- **REST Jackson**: Serialización/deserialización JSON

### Mapeo de Objetos
- **MapStruct**: Mapeo de objetos en tiempo de compilación (máxima eficiencia)

### Observabilidad
- **Micrometer + Prometheus**: Métricas de aplicación
- **OpenTelemetry**: Trazas distribuidas
- **Grafana + Loki + Tempo**: Stack completo de observabilidad

### Herramientas de Desarrollo
- **Quarkus Dev UI**: Interface de desarrollo interactiva
- **Live Reload**: Recarga en caliente durante desarrollo
- **Maven Wrapper**: Gestión consistente de dependencias

## 📋 Módulos del Sistema

### Gestión de Clientes
- Registro completo de clientes con validaciones
- Soft delete para historial de datos
- Búsqueda avanzada con filtros y paginación
- Validación de unicidad de documentos y emails

### Catálogos Maestros
- **Países**: Gestión de países con códigos ISO, monedas y prefijos telefónicos
- **Monedas**: Administración de monedas con símbolos y códigos ISO
- **Tipos de Documentos SUNAT**: Catálogo oficial de documentos tributarios
- **Tipos de Documentos del Sistema**: Documentos de identidad personalizados

### Sistema Contable
- Registro de transacciones contables
- Libro mayor y auxiliares
- Balance general y estados financieros
- Conciliaciones bancarias

### Registro de Ventas
- Comprobantes de venta (Boletas, Facturas)
- Integración con SUNAT
- Control de inventario
- Reportes de ventas

## 🚀 Desarrollo Iterativo

Este proyecto sigue un enfoque de **desarrollo incremental mediante Issues**. Cada funcionalidad, mejora o corrección se implementa a través de issues específicos en GitHub, permitiendo:

- ✅ Trazabilidad completa del desarrollo
- ✅ Revisión de código estructurada
- ✅ Documentación integrada con el código
- ✅ Control de versiones granular

## 📦 Instalación y Configuración

> **Nota**: Las instrucciones detalladas de instalación de dependencias y configuración del entorno se encuentran en el **Issue #1: Instalación de Dependencias**.

### Requisitos Previos

- Java 21 (JDK)
- Maven 3.9+
- PostgreSQL 14+
- Redis 7+
- Docker y Docker Compose (opcional, recomendado)

### Variables de Entorno

El sistema utiliza perfiles de Quarkus para diferentes entornos:

- `dev`: Desarrollo local
- `test`: Pruebas
- `prod`: Producción

## 🔐 Seguridad

- Validación de entrada en todas las capas
- Protección contra inyección SQL mediante Panache
- Soft delete para preservar historial de datos
- Validaciones de negocio en la capa de dominio

## 📊 Observabilidad

El sistema incluye un stack completo de observabilidad:

- **Logs estructurados**: JSON logging para análisis eficiente
- **Métricas**: Prometheus para monitoreo de rendimiento
- **Trazas distribuidas**: OpenTelemetry para seguimiento de requests
- **Visualización**: Grafana + Loki + Tempo para dashboards y análisis

## 🤝 Contribución

Este es un proyecto privado. El desarrollo se realiza mediante issues específicos. Para contribuir:

1. Revisa los issues abiertos
2. Asigna o crea un issue para la tarea
3. Desarrolla en una rama feature
4. Crea un Pull Request referenciando el issue

## 📄 Licencia

Copyright © 2025 Walrex. Todos los derechos reservados.

Este software es propietario y confidencial. El uso no autorizado está estrictamente prohibido.

---

**Desarrollado con ❤️ usando Quarkus**
