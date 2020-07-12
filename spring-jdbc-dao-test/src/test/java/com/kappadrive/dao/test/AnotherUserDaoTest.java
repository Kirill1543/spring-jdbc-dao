package com.kappadrive.dao.test;

import com.kappadrive.dao.test.model.User;
import com.kappadrive.dao.test.model.UserRole;
import com.kappadrive.dao.test.repository.AnotherUserDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@Sql("classpath:/ddl/create_users.sql")
class AnotherUserDaoTest {

    @Autowired
    private AnotherUserDao userDao;

    @Test
    void test() {
        User user = new User();
        user.setName("Kenny");
        user.setOtherName("A");
        userDao.addNewUser(user);

        assertThat(userDao.getUser(1L))
                .isPresent()
                .hasValueSatisfying(u -> assertAll(
                        () -> assertThat(u.getName()).isEqualTo("Kenny"),
                        () -> assertThat(u.getOtherName()).isEqualTo("A"),
                        () -> assertThat(u.getRole()).isNull()
                ));

        User user2 = new User();
        user2.setName("Kyle");
        user2.setOtherName("B");
        userDao.addNewUser(user2);

        assertThat(userDao.getUser(2L))
                .isPresent()
                .hasValueSatisfying(u -> assertAll(
                        () -> assertThat(u.getName()).isEqualTo("Kyle"),
                        () -> assertThat(u.getOtherName()).isEqualTo("B"),
                        () -> assertThat(u.getRole()).isNull()
                ));

        user2.setOtherName("A");
        userDao.changeUser(user2);

        assertThat(userDao.getUser(2L))
                .isPresent()
                .hasValueSatisfying(u -> assertAll(
                        () -> assertThat(u.getName()).isEqualTo("Kyle"),
                        () -> assertThat(u.getOtherName()).isEqualTo("A"),
                        () -> assertThat(u.getRole()).isNull()
                ));

        userDao.removeFullFamily("A");
        assertThat(userDao.getUser(1L)).isEmpty();
        assertThat(userDao.getUser(2L)).isEmpty();
    }

    @Test
    void testWithValues() {
        User user = new User();
        user.setName("Kenny");
        user.setOtherName("A");
        user.setRole(UserRole.ADMIN);
        userDao.addNewUser(user);

        User user2 = userDao.add("Kyle");
        userDao.setFamily("A", "Kyle");
        user2.setOtherName("A");

        assertThat(userDao.restoreAllUsers("A"))
                .hasSize(2)
                .contains(user, user2);

        userDao.remove("Kenny", UserRole.ADMIN);

        assertThat(userDao.restoreAllUsers("A"))
                .hasSize(1)
                .contains(user2);
    }
}
