package com.kappadrive.dao.test.repository;

import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.api.JdbcDao;
import com.kappadrive.dao.test.model.User;
import com.kappadrive.dao.test.model.UserRole;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

@GenerateDao
public interface UserDao extends JdbcDao<User> {

    @Nonnull
    Optional<User> findById(@Nonnull Long id);

    @Nonnull
    List<User> findByName(@Nonnull String name);

    @Nonnull
    List<User> findByNameAndRole(@Nonnull String name, @Nonnull UserRole role);

    void delete(@Nonnull Long id);
}
