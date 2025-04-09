# User Management System

A complete user management system with a REST API backend and Flutter mobile application.

## Project Structure

The project consists of two main parts:

1. Backend REST API (Java)
2. Flutter Mobile Application

### Backend REST API

The backend is built with Java and provides the following features:

- User registration and authentication
- Role-based access control
- User profile management
- Token-based authentication
- Google Cloud Datastore integration

### Flutter Mobile Application

The Flutter app provides a mobile interface for the user management system with the following features:

- User registration
- User login
- Role-based access control
- User profile management
- Admin controls (for ADMIN and BACKOFFICE roles)

## Getting Started

### Backend Setup

1. Make sure you have Java 21 installed
2. Install Maven
3. Configure Google Cloud credentials
4. Run the application:
   ```bash
   mvn appengine:run
   ```

### Flutter App Setup

1. Navigate to the Flutter app directory:
   ```bash
   cd flutter_app
   ```

2. Install dependencies:
   ```bash
   flutter pub get
   ```

3. Run the app:
   ```bash
   flutter run
   ```

## API Endpoints

### Authentication

- `POST /rest/register` - Register a new user
- `POST /rest/login` - Login user
- `GET /rest/logout` - Logout user

### User Management

- `PUT /rest/user/{username}/role` - Change user role
- `PUT /rest/user/{username}/state` - Change user account state
- `DELETE /rest/user/{username}` - Remove user account

## Testing

### Backend Tests

```bash
mvn test
```

### Flutter Tests

```bash
cd flutter_app
flutter test
```

## Deployment

### Backend Deployment

```bash
mvn appengine:deploy
```

### Flutter App Deployment

```bash
cd flutter_app
flutter build apk  # For Android
flutter build ios  # For iOS
```

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

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

### Login (OP2)
- **POST** `/rest/login`
- Body (JSON):
```json
{
    "username": "string",
    "password": "string"
}
```

### Mudança de Role (OP3)
- **POST** `/rest/changerole/`
- Body (JSON):
```json
{
    "targetUsername": "string",
    "newRole": "string"
}
```

### Mudança de Estado da Conta (OP3)
- **POST** `/rest/changeAccountState`
- Body (JSON):
```json
{
    "targetUsername": "string",
    "newState": "string"
}
```

### Remover Conta de Usuário (OP4)
- **POST** `/rest/removeUserAccount`
- Body (JSON):
```json
{
    "targetUsername": "string"
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

