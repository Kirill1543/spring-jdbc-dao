package com.kappadrive.dao.test;

import com.kappadrive.dao.test.model.User;
import com.kappadrive.dao.test.repository.AbstractSomeUserDao;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public class SomeUserDaoImpl extends AbstractSomeUserDao {

    protected SomeUserDaoImpl(NamedParameterJdbcTemplate template) {
        super(template);
    }

    @Nonnull
    @Override
    public Optional<User> findById(@Nonnull Long id) {
        return Optional.empty();
    }
}
