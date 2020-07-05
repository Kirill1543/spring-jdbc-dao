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
    void testInsertAndFind() {
        User user = new User();
        user.setName("User");
        user.setOtherName("Kyle");
        User inserted = userDao.insert(user);
        assertThat(inserted.getId()).isEqualTo(1);
        assertThat(userDao.findById(1L))
                .isPresent()
                .hasValueSatisfying(u -> assertAll(
                        () -> assertThat(u.getName()).isEqualTo("User"),
                        () -> assertThat(u.getOtherName()).isEqualTo("Kyle"),
                        () -> assertThat(u.getRole()).isNull()
                ));

        user.setName("User2");
        user.setRole(UserRole.ADMIN);
        User inserted2 = userDao.insert(user);
        assertThat(inserted2.getId()).isEqualTo(2);
        assertThat(userDao.findById(2L))
                .isPresent()
                .hasValueSatisfying(u -> assertAll(
                        () -> assertThat(u.getName()).isEqualTo("User2"),
                        () -> assertThat(u.getOtherName()).isEqualTo("Kyle"),
                        () -> assertThat(u.getRole()).isEqualTo(UserRole.ADMIN)
                ));
    }

    @Test
    void testFindAll() {
        User user = new User();
        user.setName("Kyle");
        user.setOtherName("Jersey");
        userDao.insert(user);

        assertThat(userDao.findAll())
                .hasSize(1)
                .containsExactly(user);

        User user2 = new User();
        user2.setName("Eric");
        user2.setOtherName("Fat");
        userDao.insert(user2);

        assertThat(userDao.findAll())
                .hasSize(2)
                .containsExactly(user, user2);
    }

    @Test
    void testUpdate() {
        User user = new User();
        user.setName("Kyle");
        user.setOtherName("Jersey");
        userDao.insert(user);

        user.setRole(UserRole.ADMIN);
        user.setOtherName("B");
        userDao.update(user);

        assertThat(userDao.findById(1L))
                .isPresent()
                .hasValueSatisfying(u -> assertAll(
                        () -> assertThat(u.getName()).isEqualTo("Kyle"),
                        () -> assertThat(u.getOtherName()).isEqualTo("B"),
                        () -> assertThat(u.getRole()).isEqualTo(UserRole.ADMIN)
                ));
    }

    @Test
    void testDelete() {
        User user = new User();
        user.setName("Kyle");
        user.setOtherName("Jersey");
        userDao.insert(user);

        userDao.delete(1L);
        assertThat(userDao.findAll()).isEmpty();
    }
}
