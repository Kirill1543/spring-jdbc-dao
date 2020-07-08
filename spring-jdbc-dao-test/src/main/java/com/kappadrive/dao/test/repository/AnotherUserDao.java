package com.kappadrive.dao.test.repository;

import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.api.Query;
import com.kappadrive.dao.test.model.User;

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
}
