# Charix 🦀

Serializable representation of KSP models

## How to produce a release

### 1. Update the GitHub credentials in the `local.properties` file

```properties
githubUsername=USERNAME
githubPassword=TOKEN
```

> [!WARNING]
> The `githubPassword` should be a GitHub token with the `write:packages` scope.

### 2. Create a new tag

```zsh
git tag -a VERSION -m "Release VERSION"
```

### 3. Run the following command

```zsh
./gradlew library:publishAllPublicationsToGithubPackagesRepository -PgitTag=VERSION
```

### 4. Push the tag

```zsh
git push origin VERSION
```