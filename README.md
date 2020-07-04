# spring-jdbc-dao
This is library to generate DAO structures, based on Spring jdbc templates, from interfaces and entities

## DAO
Each generated DAO should extend `JdbcDao` class and have `@GenerateDao` annotation:
```java
@GenerateDao
public interface UserDao extends JdbcDao<User, Long> {

    @Nonnull
    Optional<User> findById(Long id);

    @Nonnull
    List<User> findAllByLastName(String lastName);
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
