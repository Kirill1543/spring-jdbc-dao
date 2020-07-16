## 1.0
First release with base functional.
# Added
 - Main `@GeneratedDao` which triggers annotation processor to generate DAOs
 from entities classes. Entities should be classes with fields/getters/setters.
 Generation happens only for interfaces DAOs.
 - `@Id`/`@Column`/`@GeneratedValue`/`@Table` to map entity type to database structure.
 - `@Query.Select`/`@Query.Insert`/`@Query.Update`/`@Query.Delete`/`@Query.Param` to
 configure custom methods for generation.