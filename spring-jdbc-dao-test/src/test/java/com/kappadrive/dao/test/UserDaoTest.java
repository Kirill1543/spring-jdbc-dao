package com.kappadrive.dao.test;

import com.kappadrive.dao.test.model.User;
import com.kappadrive.dao.test.model.UserRole;
import com.kappadrive.dao.test.repository.UserDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@Sql("classpath:/ddl/create_users.sql")
class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Test
    void testEmpty() {
        assertThat(userDao.findAll()).isEmpty();
        assertThat(userDao.findById(1L)).isEmpty();
    }

    @Test
    void testInsert() {
        User user = new User();
        user.setName("User");
        user.setOtherName("Kyle");
        User inserted = userDao.insert(user);
        assertThat(inserted.getId()).isEqualTo(1);
        assertThat(userDao.findById(1L))
                .isPresent()
                .hasValueSatisfying(v -> assertAll(
                        () -> assertThat(v.getName()).isEqualTo("User"),
                        () -> assertThat(v.getOtherName()).isEqualTo("Kyle"),
                        () -> assertThat(v.getRole()).isNull()
                ));

        user.setName("User2");
        user.setRole(UserRole.ADMIN);
        User inserted2 = userDao.insert(user);
        assertThat(inserted2.getId()).isEqualTo(2);
        assertThat(userDao.findById(2L))
                .isPresent()
                .hasValueSatisfying(v -> assertAll(
                        () -> assertThat(v.getName()).isEqualTo("User2"),
                        () -> assertThat(v.getOtherName()).isEqualTo("Kyle"),
                        () -> assertThat(v.getRole()).isEqualTo(UserRole.ADMIN)
                ));
    }
}
