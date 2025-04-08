# APDC-2025-Individual-61905

Projeto individual para a disciplina de ADC (2024/25).

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

