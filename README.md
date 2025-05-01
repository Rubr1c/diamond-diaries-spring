# Diamond Diaries

A Spring Boot application for journaling, featuring user authentication, rich text editing, and AI integration.

## Requirements

- **Docker & Docker Compose:** For containerizing and running the application and its database.
- **Java 17:** Required if building or running the application outside of Docker.
- **Maven:** Required for dependency management and building if running outside of Docker.
- **Environment Variables:** You need to create a `.env` file in the project root based on the provided `.env.example`. Fill in the necessary credentials and configuration values (Database, JWT Secret, Mail Server, Google OAuth, AWS Keys, Gemini API Key, etc.).

## How to Run

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd Journal-Spring
    ```
2.  **Create and configure the environment file:**
    - Copy `.env.example` to `.env`.
    - Open `.env` and fill in all the required environment variable values.
3.  **Build and run with Docker Compose:**
    ```bash
    docker compose up --build -d
    ```
    - The `-d` flag runs the containers in detached mode (in the background).
    - The first build might take some time as it downloads dependencies.
4.  **Access the application:**
    Once the containers are up and running, the application should be accessible at `http://localhost:8080` (or the port configured if different).

## Stopping the Application

To stop the running containers:

```bash
docker compose down
```
