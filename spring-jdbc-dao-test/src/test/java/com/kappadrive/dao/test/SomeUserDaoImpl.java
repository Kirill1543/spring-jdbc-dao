package com.kappadrive.dao.test;

import com.kappadrive.dao.test.repository.AbstractSomeUserDao;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SomeUserDaoImpl extends AbstractSomeUserDao {

    protected SomeUserDaoImpl(NamedParameterJdbcTemplate template) {
        super(template);
    }
}
