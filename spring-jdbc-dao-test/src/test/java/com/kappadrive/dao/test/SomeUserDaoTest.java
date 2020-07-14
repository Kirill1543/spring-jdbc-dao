package com.kappadrive.dao.test;

import com.kappadrive.dao.test.model.User;
import com.kappadrive.dao.test.model.UserRole;
import com.kappadrive.dao.test.repository.SomeUserDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql("classpath:/ddl/create_users.sql")
class SomeUserDaoTest {

    @Autowired
    private SomeUserDao userDao;

    @Test
    void test() {
        User user = new User();
        user.setName("Eric");
        user.setOtherName("C");
        user.setRole(UserRole.USER);

        User inserted = userDao.insert(user);

        assertThat(userDao.findAll())
                .hasSize(1)
                .containsExactly(inserted);

        assertThat(userDao.findByName("Eric"))
                .hasSize(1)
                .containsExactly(inserted);
    }
}
