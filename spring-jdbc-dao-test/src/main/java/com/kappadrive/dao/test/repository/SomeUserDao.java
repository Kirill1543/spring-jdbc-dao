package com.kappadrive.dao.test.repository;

import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.api.SkipGeneration;
import com.kappadrive.dao.test.model.User;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

@GenerateDao(value = User.class, makeAbstract = true)
public interface SomeUserDao {

    @Nonnull
    Iterable<User> findAll();

    @Nonnull
    User insert(@Nonnull User user);

    @Nonnull
    List<User> findByName(@Nonnull String name);

    @Nonnull
    @SkipGeneration
    Optional<User> findById(@Nonnull Long id);
}
