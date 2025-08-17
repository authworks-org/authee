# Contributing to Authee

Welcome! Thank you for your interest in contributing to **Authee** — an open-source, centralized authentication service built in Java with Spring Boot.

We welcome all contributions: code, documentation, bug reports, ideas, and improvements. Please read the guidelines below before you start.

---

## 1. Getting Started

- **Fork** this repository and create your branch from `master` or the latest development branch.
- Ensure you have Java 17+ and Gradle installed.
- Clone your fork locally:
```

git clone https://github.com/your-username/authee.git
cd authee

```

---

## 2. Setting Up the Project

- To build and run tests:
```

./gradlew clean build

```
- To start the application locally:
```

./gradlew bootRun

```
- Example configurations can be found in `/src/main/resources/application.properties`.

---

## 3. Coding Guidelines

- **Follow Java best practices** and [Google Java Style](https://google.github.io/styleguide/javaguide.html).
- Use descriptive commit messages.
- Ensure new code is covered by unit/integration tests (`/src/test/java`).
- All public classes and methods must have Javadoc comments.
- Organize imports and remove unused code before submission.
- External dependencies must use open-source licenses compatible with Apache-2.0/MIT.

---

## 4. Pull Request Process

1. **Fork & branch:**  
  Name your branch descriptively, e.g., `feature/saml-plugin`, `bugfix/token-expiry`, `docs/readme-update`.

2. **Test before PR:**  
  All tests should pass locally. Cover new features/bugs with appropriate test cases.

3. **Check your code:**  
  ```
  ./gradlew check
  ```

4. **Submit a Pull Request:**  
  - Go to GitHub and open a PR to `master` or the active dev branch.
  - Describe the purpose, context, and changes in the PR.
  - Tag any related issues in your PR description.

5. **PR review:**  
  - Project maintainers will review your PR and may ask for changes or clarifications.
  - Address review feedback promptly.

---

## 5. Bug Reports & Feature Requests

- Use [GitHub Issues](https://github.com/YOUR_ORG/authee/issues) for reporting bugs, requesting features, or asking questions.
- Please provide detailed steps to reproduce bugs and your environment if applicable.

---

## 6. Documentation

- Update the `README.md`, Javadocs, and `/docs/` content for any user-facing changes.
- Add or update API reference guides if you add new endpoints.

---

## 7. Community & Etiquette

- Be respectful, inclusive, and helpful to other contributors.
- Follow the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/2/0/code_of_conduct/).

---

## 8. License

By contributing, you agree that your code will be licensed under the project’s [LICENSE](./LICENSE).

---

**Thank you for helping make Authee a better project!**
```

Feel free to modify this template as needed for your organization or specifics of your workflow.

