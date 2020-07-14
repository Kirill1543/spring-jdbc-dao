package com.kappadrive.dao.test.repository;

import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.test.model.User;

import javax.annotation.Nonnull;
import java.util.List;

@GenerateDao(value = User.class, makeAbstract = true)
public interface SomeUserDao {

    @Nonnull
    Iterable<User> findAll();

    @Nonnull
    User insert(@Nonnull User user);

    @Nonnull
    List<User> findByName(@Nonnull String name);
}
