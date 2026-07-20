# 🚀 CollaB Tasks - Backend

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green?logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-darkgreen?logo=springsecurity)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Maven](https://img.shields.io/badge/Maven-3.x-red?logo=apachemaven)
![License](https://img.shields.io/badge/license-MIT-green)

---

# 📖 Présentation

**CollaB Tasks Backend** est l'API REST officielle de la plateforme collaborative **CollaB Tasks**.

Cette API fournit les services nécessaires pour :

- gérer les utilisateurs
- gérer les projets
- gérer les tâches
- gérer les rôles et permissions
- gérer l'authentification
- sécuriser les accès
- fournir les données nécessaires au frontend React


Le backend est développé avec :

- **Java 17**
- **Spring Boot**
- **Spring Security**
- **PostgreSQL**
- **Architecture MVC**
- **Programmation Orientée Objet**

---

# 🛠️ Stack technique

| Technologie | Description |
|---|---|
| Java 17 | Langage principal |
| Spring Boot 3.x | Framework backend |
| Spring MVC | Architecture applicative |
| Spring Security | Sécurité API |
| JWT | Authentification |
| Cookies HttpOnly | Stockage sécurisé des tokens |
| PostgreSQL | Base de données relationnelle |
| Hibernate / JPA | ORM |
| Maven | Gestion dépendances |
| Lombok | Réduction du code répétitif |
| Validation API | Contrôle des données entrantes |

---

# 🏗️ Architecture Backend

Le projet respecte une architecture **MVC (Model - View - Controller)** adaptée aux API REST.

Organisation :

```text
src/main/java/com/collab/tasks

│
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   └── ApplicationConfig.java
│
├── controllers/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── ProjectController.java
│   └── TaskController.java
│
├── services/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── ProjectService.java
│   ├── TaskService.java
│   │
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── UserServiceImpl.java
│       ├── ProjectServiceImpl.java
│       └── TaskServiceImpl.java
│
├── repositories/
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   └── TaskRepository.java
│
├── entities/
│   ├── User.java
│   ├── Role.java
│   ├── Project.java
│   └── Task.java
│
├── dto/
│   │
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── ProjectRequest.java
│   │   └── TaskRequest.java
│   │
│   └── response/
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── ProjectResponse.java
│       └── TaskResponse.java
│
├── mapper/
│   ├── UserMapper.java
│   ├── ProjectMapper.java
│   ├── TaskMapper.java
│   └── RoleMapper.java
│
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetailsService.java
│   └── SecurityUtils.java
│
├── exceptions/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── BadRequestException.java
│
├── utils/
│   └── Constants.java
│
└── CollabTasksApplication.java
```

---

---

# 🏛️ Principe MVC


## Model

Représente les données métier.

Contient :

- Entities JPA
- Relations entre objets
- Règles métier


Exemple :

```
User

Project

Task

Role
```

---

## Controller

Responsable de :

- recevoir les requêtes HTTP
- valider les entrées
- retourner les réponses


Exemple :

```
POST /api/auth/login

GET /api/projects

POST /api/tasks
```

---

## Service

Contient la logique métier.

Responsabilités :

- traitement métier
- transformation Entity ↔ DTO
- validation métier
- orchestration des repositories


---

## Repository

Responsable de l'accès aux données.

Utilise :

- Spring Data JPA
- Hibernate
- PostgreSQL


---

# 🧱 Programmation Orientée Objet


Le projet applique les principes POO :

## Encapsulation

Les attributs des classes sont privés.

```java
private String email;
private String password;
```


---

## Héritage

Réutilisation des comportements communs.

Exemple :

```
BaseEntity

      ↓

User

Project

Task
```


---

## Polymorphisme

Utilisation des interfaces et implémentations.

Exemple :

```
UserService

      ↓

UserServiceImpl
```


---

## Abstraction

Séparation entre contrat et implémentation.

Exemple :

```
interface AuthService

class AuthServiceImpl
```

---

# 📦 Gestion des DTO


Le backend utilise le pattern **DTO (Data Transfer Object)**.

Les DTO permettent de :

- ne pas exposer directement les entités
- sécuriser les données échangées
- contrôler les informations envoyées au frontend


Structure :

```
dto/

├── request

│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   └── TaskRequest.java


└── response

    ├── UserResponse.java
    ├── ProjectResponse.java
    └── TaskResponse.java
```


Exemple :

```java
public class UserResponse {

    private Long id;

    private String username;

    private String email;

    private String role;
}
```

---

# 🔐 Authentification et Sécurité


Le système utilise :

- Spring Security
- JWT
- Cookies HttpOnly
- Refresh Token
- RBAC


Architecture :

```
Client React

      │

      ▼

Login API

      │

      ▼

Spring Security

      │

      ▼

JWT Generation

      │

      ▼

HttpOnly Cookie

      │

      ▼

API protégée
```

---

# 🍪 Gestion des Cookies


Les tokens JWT sont stockés dans des cookies sécurisés :

```
accessToken

refreshToken
```


Configuration :

- HttpOnly : activé
- Secure : activé en production
- SameSite : configuré
- Expiration contrôlée


Le frontend ne manipule jamais directement les tokens.


---

# 🛡️ Spring Security


Le backend utilise :

- AuthenticationManager
- UserDetailsService
- JWT Filter
- Security Filter Chain


Les routes publiques :

```
POST /api/auth/login

POST /api/auth/register
```


Les routes protégées :

```
GET /api/users

POST /api/projects

POST /api/tasks
```


---

# 👥 Gestion des rôles (RBAC)


Les accès sont contrôlés selon les rôles :


| Rôle | Permissions |
|-|-|
| SUPER_ADMIN | Administration complète |
| ADMIN | Gestion utilisateurs et projets |
| USER | Utilisation standard |


Exemple :

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/users")
public ResponseEntity<?> getUsers(){

}
```

---

# 🗄️ Base de données


Base utilisée :

```
PostgreSQL
```


Exemple de configuration :

```properties
spring.datasource.url=${DB_URL}

spring.datasource.username=${DB_USERNAME}

spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
```

---

# ⚙️ Installation


## Prérequis

Installer :

- Java 17
- Maven
- PostgreSQL


Vérification :

```bash
java -version

mvn -version
```

---

# 📥 Cloner le projet


```bash
git clone https://github.com/votre-compte/collab-tasks-backend.git

cd collab-tasks-backend
```

---

# 🔐 Variables d'environnement


Créer :

```
.env
```


Exemple :

```env
DB_URL=jdbc:postgresql://localhost:5432/collab_tasks

DB_USERNAME=postgres

DB_PASSWORD=password


JWT_SECRET_KEY=your_secret_key
```

---

# ▶️ Lancer l'application


Avec Maven :

```bash
mvn spring-boot:run
```


L'API sera disponible :

```
http://localhost:8090/api
```

---

# 📡 Endpoints principaux


## Authentification


```
POST /api/auth/register

POST /api/auth/login

POST /api/auth/logout

POST /api/auth/refresh
```


---

## Utilisateurs


```
GET    /api/users

GET    /api/users/{id}

PUT    /api/users/{id}

DELETE /api/users/{id}
```

---

## Projets


```
GET    /api/projects

POST   /api/projects

PUT    /api/projects/{id}

DELETE /api/projects/{id}
```

---

## Tâches


```
GET    /api/tasks

POST   /api/tasks

PUT    /api/tasks/{id}

DELETE /api/tasks/{id}
```

---

# 🧪 Tests


Frameworks :

- JUnit 5
- Mockito
- Spring Boot Test


Tests réalisés :

- tests services
- tests controllers
- tests sécurité
- tests repositories


Exemple :

```
src/test

├── service

├── controller

└── security
```

---

# 🐳 Déploiement Docker


Exemple Dockerfile :

```dockerfile
FROM eclipse-temurin:17

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java","-jar","app.jar"]
```


Build :

```bash
docker build -t collab-tasks-backend .
```


Run :

```bash
docker run -p 8090:8090 collab-tasks-backend
```

---

# 🔄 Workflow Git


Créer une branche :

```bash
git checkout -b feature/nouvelle-fonctionnalite
```


Commit :

```bash
git commit -m "Ajout fonctionnalité"
```


Push :

```bash
git push origin feature/nouvelle-fonctionnalite
```

---

# 📄 Licence


Projet développé dans le cadre de **CollaB Tasks**.


MIT License


© 2026 CollaB Tasks
