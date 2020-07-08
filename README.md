# spring-jdbc-dao
This is library to generate DAO structures, based on Spring jdbc templates, from interfaces and entities

## DAO
To generate implementation for DAO `@GenerateDao` annotation should be used:
```java
@GenerateDao(User.class)
public interface UserDao {

    @Nonnull
    Optional<User> findById(Long id);

    @Nonnull
    List<User> findAllByLastName(String lastName);

    @Nonnull
    <S extends User> S insert(S user);
}
```

## Entities
This library suppose to work with entities as regular java POJOs with getters and setters:
```java
public class User {
    private Long id;
    private String firstName;
    private String lastName;
    // getters and setters omitted for readability
}
```

## Supported methods
There are 4 supported types of methods
### SELECT
  - Naming pattern: `find*`
  - Return types:
    - `Optional<E>` for single return
    - `List/Collection/Iterable<E>` for multiple return
  - Parameters: zero or any number of parameters with types and names 
  matching `Entity` fields
  - Example:
```java
public class User {
    private Long id;
    private String firstName;
    private String lastName;
}
@GenerateDao(User.class)
public interface UserDao {
    // supported
    List<User> findAll();

    // supported
    Optional<User> findById(Long id);

    // supported
    List<User> findByFirstName(String firstName);

    // not supported
    List<User> findByLastName(String v);
}
``` 
### INSERT
  - Naming pattern: `insert*`
  - Return types: 
    - `Entity` or generic `E extends Entity` for single `Entity` parameter
    - `Entity` for other cases. In this case new `Entity` will be created 
  - Parameters:
    - single parameter of type `Entity` or `E extends Entity`
    - single or multiple parameters with types and names matching `Entity` fields
  - Example:
```java
public class User {
    private String firstName;
    private String lastName;
}
@GenerateDao(User.class)
public interface UserDao {
    <E extends User> E insert(E user);

    User insert(String firstName, String lastName);
}
``` 
### UPDATE
- Naming pattern: `update*`
  - Return types: only `void`
  - Parameters: single parameter of type `Entity`
  - Example:
```java
public class User {
    private String firstName;
    private String lastName;
}
@GenerateDao(User.class)
public interface UserDao {
    void update (E user);
}
``` 
### DELETE
  - Naming pattern: `delete*`
  - Return types: only `void`
  - Parameters:
    - single or multiple parameters with types and names matching `Entity` fields
  - Example:
```java
public class User {
    private String firstName;
    private String lastName;
}
@GenerateDao(User.class)
public interface UserDao {
    void deleteAll();

    void deleteByLastName(String lastName);
}
```
## Notes
1. It was decided not to follow javax.persistence API due to poor compatibility
with this library targets
2. It is recommended for java compiler to set parameter names from source version instead generated one.
Example for gradle:
```groovy
compileJava {
    options.compilerArgs << '-parameters'
}
```
## TODO checklist for first complete release:
  - Add alternative annotations which can be used instead method name pattern
  - Add parameter annotation
  - Add possibility to create custom hardcoded queries for each method type
  - Make possible to use such queries from properties via `@Value` annotation
  - Create alternative for dynamic sql queries instead of static generated one
  - Simplify working with entities, which are result of join statements