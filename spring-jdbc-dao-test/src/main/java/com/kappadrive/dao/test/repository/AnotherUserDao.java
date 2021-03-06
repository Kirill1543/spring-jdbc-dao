package com.kappadrive.dao.test.repository;

import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.api.Query;
import com.kappadrive.dao.test.model.User;
import com.kappadrive.dao.test.model.UserRole;

import java.sql.Types;
import java.util.List;
import java.util.Optional;

@GenerateDao(User.class)
public interface AnotherUserDao {

    @Query.Select
    Optional<User> getUser(Long id);

    @Query.Insert
    User addNewUser(User user);

    @Query.Update
    void changeUser(User user);

    @Query.Delete
    void removeFullFamily(String otherName);

    @Query.Select("SELECT * FROM users WHERE family_name = :value")
    List<User> restoreAllUsers(String value);

    @Query.Insert("INSERT INTO users (name) VALUES (:value)")
    User add(@Query.Param("value") String name);

    @Query.Update("UPDATE users SET family_name = :family where name = :name")
    void setFamily(String family, String name);

    @Query.Delete("DELETE FROM users WHERE name = :name AND role = :r")
    void remove(String name, @Query.Param(value = "r", type = Types.VARCHAR) UserRole userRole);
}
