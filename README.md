# APDC Individual Project

This project is a RESTful web service built with Java and Google App Engine, implementing user management and authentication functionality.

## Features

- User registration and authentication
- Role-based access control
- Token-based authentication
- User management (list, update, delete)
- Role and state management for users
- Secure password handling with SHA-512 hashing

## Technologies Used

- Java 21
- Google App Engine (Standard Environment)
- Google Cloud Datastore
- Jersey (JAX-RS implementation)
- Maven
- Gson for JSON processing
- Apache Commons Codec for hashing

## Prerequisites

- Java Development Kit (JDK) 21
- Apache Maven
- Google Cloud SDK
- A Google Cloud Platform project with the following APIs enabled:
  - App Engine Admin API
  - Cloud Datastore API
  - Cloud Build API

## Setup and Deployment

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd <project-directory>
   ```

2. Configure your Google Cloud project:
   ```bash
   gcloud config set project YOUR_PROJECT_ID
   ```

3. Update the `pom.xml` file with your Google Cloud project ID:
   ```xml
   <configuration>
       <projectId>YOUR_PROJECT_ID</projectId>
   </configuration>
   ```

4. Build the project:
   ```bash
   mvn clean package
   ```

5. Deploy to Google App Engine:
   ```bash
   mvn appengine:deploy
   ```

## API Endpoints

### Authentication
- `POST /rest/auth/register` - Register a new user
- `POST /rest/auth/login` - Login and get authentication token
- `POST /rest/auth/logout` - Logout and invalidate token

### User Management
- `GET /rest/user/listusers` - List users (requires authentication)
- `POST /rest/user/changerole` - Change user role (requires ADMIN/BACKOFFICE)
- `POST /rest/user/changestate` - Change user state (requires ADMIN/BACKOFFICE)
- `DELETE /rest/user/{username}` - Delete user (requires ADMIN/BACKOFFICE)

## Default Admin Account

On first deployment, a root admin account is created with the following credentials:
- Username: root
- Password: password123

**Important**: Change the password immediately after first login.

## Security Notes

- All passwords are hashed using SHA-512 before storage
- Authentication tokens expire after 1 hour
- HTTPS is enforced for all endpoints
- Role-based access control is strictly enforced

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Estrutura do Projeto

O projeto é uma aplicação REST que utiliza:
- Google App Engine
- Google Datastore para persistência
- Jersey para endpoints REST
- Sistema de autenticação com tokens

## Endpoints Disponíveis

### Registro de Usuário (OP1)
- **POST** `/rest/register`
- Body (JSON):
```json
{
    "username": "string",           // Obrigatório
    "password": "string",           // Obrigatório (min 8 chars, maiúsculas, minúsculas, números e especiais)
    "email": "string",             // Obrigatório (formato: usuario@dominio.tld)
    "fullName": "string",          // Obrigatório
    "phone": "string",             // Obrigatório (formato: +3512895629)
    "profile": "string",           // Obrigatório ("public" ou "private")
    "citizenCardNumber": "string", // Opcional
    "role": "string",              // Opcional (padrão: "enduser")
    "nif": "string",               // Opcional
    "employer": "string",          // Opcional
    "jobTitle": "string",          // Opcional
    "address": "string",           // Opcional
    "employerNif": "string",       // Opcional
    "photoUrl": "string"           // Opcional (URL para foto em JPEG)
}
```

### Login
- **POST** `/rest/login`
- Body (JSON):
```json
{
    "username": "string",
    "password": "string"
}
```

## Como Executar

1. Clone o repositório
2. Configure as credenciais do Google Cloud
3. Execute localmente:
```bash
mvn appengine:run
```
4. Para deploy:
```bash
mvn appengine:deploy
```

## Testando com Postman

1. Importe a coleção do Postman (disponível na pasta `INFO`)
2. Configure as variáveis de ambiente:
   - `baseUrl`: URL base da aplicação
   - `token`: Token de autenticação (obtido após login)

## Autenticação

Todas as operações (exceto login e registro) requerem um token de autenticação no header:
```
Authorization: Bearer <token>
```

O token é obtido após um login bem-sucedido e expira em 2 horas.

## Usuário Root

A aplicação cria automaticamente um usuário root com as seguintes credenciais:
- Username: `root`
- Password: `2025adcAVALind!!!`
- Role: `ADMIN`
- Estado: `ATIVADA`

## Validações

### Email
- Deve seguir o formato: usuario@dominio.tld
- Exemplo: petermurphy3456@campus.fct.unl.pt

### Senha
- Mínimo 8 caracteres
- Deve conter:
  - Letras maiúsculas
  - Letras minúsculas
  - Números
  - Caracteres especiais
- Exemplo: 2025adcAVALind!!!

### Telefone
- Deve conter pelo menos 9 dígitos
- Pode incluir o código do país
- Exemplo: +3512895629

### Perfil
- Deve ser "public" ou "private"

### Estado da Conta
- Novas contas são criadas como "DESATIVADA"
- Pode ser alterado para "ATIVADA" ou "SUSPENSA"
- Apenas usuários com role ADMIN ou BACKOFFICE podem alterar o estado

