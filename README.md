\# Production-Grade E-Commerce Platform



A full-stack e-commerce application built with \*\*Spring Boot\*\* and \*\*React\*\*, designed to provide a foundation for a secure, maintainable online shopping experience.



\## Tech Stack



\### Backend

\- Java

\- Spring Boot 3.5.3

\- Spring Security and JWT-based authentication

\- Spring Data JPA / Hibernate

\- PostgreSQL

\- Maven



\### Frontend

\- React

\- JavaScript

\- Redux for state management

\- Axios for API communication

\- Vite



\## Project Structure



```text

Production-grade-Ecommerce/

├── Backend/

│   └── sb-ecommerce/       # Spring Boot REST API

├── Frontend/

│   └── ecom-frontend/      # React frontend

├── .gitignore

└── README.md

```



\## Getting Started



\### Prerequisites



Install the following before running the project:



\- Java Development Kit compatible with the backend configuration

\- Node.js and npm

\- PostgreSQL

\- Git



\### 1. Clone the repository



```bash

git clone https://github.com/madhvendra1027/Production-grade-Ecommerce.git

cd Production-grade-Ecommerce

```



\### 2. Run the backend



```bash

cd Backend/sb-ecommerce

```



Configure the database connection and other required environment-specific settings using the project's configuration files and environment variables.



On Windows, run:



```powershell

.\\mvnw.cmd spring-boot:run

```



\### 3. Run the frontend



Open another terminal:



```bash

cd Frontend/ecom-frontend

npm install

npm run dev

```



Configure the frontend API URL to point to your running backend.



\## Configuration and Security



\- Keep credentials, database passwords, JWT secrets, and other sensitive configuration out of version control.

\- Use the provided `.env.example` files as references where applicable.

\- Configure the backend and frontend URLs for your local environment.



\## Development



The backend and frontend are maintained in separate directories within a single repository. This makes it possible to develop and test each application independently while keeping the full-stack project together.



\## Author



\*\*Madhvendra Srivastav\*\*



GitHub: \[@madhvendra1027](https://github.com/madhvendra1027)



\---



\*This project is under active development.\*



