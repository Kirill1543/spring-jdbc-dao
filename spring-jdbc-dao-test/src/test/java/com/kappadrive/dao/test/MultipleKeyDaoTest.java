package com.kappadrive.dao.test;

import com.kappadrive.dao.test.model.MultipleKey;
import com.kappadrive.dao.test.repository.MultipleKeyDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@Sql("classpath:/ddl/create_multiple.sql")
class MultipleKeyDaoTest {

    @Autowired
    private MultipleKeyDao multipleKeyDao;

    @Test
    void test() {
        MultipleKey multipleKey = multipleKeyDao.insert(1L, 2L);
        assertThat(multipleKey)
                .satisfies(m -> assertAll(
                        () -> assertThat(m.getId()).isEqualTo(1),
                        () -> assertThat(m.getOtherId()).isEqualTo(2),
                        () -> assertThat(m.getSequence()).isEqualTo(1)
                ));
        multipleKeyDao.insert(2L, 2L);
        multipleKeyDao.insert(2L, 1L);

        assertThat(multipleKeyDao.find(1L, 2L))
                .isPresent()
                .hasValueSatisfying(m -> assertAll(
                        () -> assertThat(m.getId()).isEqualTo(1),
                        () -> assertThat(m.getOtherId()).isEqualTo(2),
                        () -> assertThat(m.getSequence()).isEqualTo(1)
                ));
        assertThat(multipleKeyDao.find(2L, 2L))
                .isPresent()
                .hasValueSatisfying(m -> assertAll(
                        () -> assertThat(m.getId()).isEqualTo(2),
                        () -> assertThat(m.getOtherId()).isEqualTo(2),
                        () -> assertThat(m.getSequence()).isEqualTo(2)
                ));
        assertThat(multipleKeyDao.find(2L, 1L))
                .isPresent()
                .hasValueSatisfying(m -> assertAll(
                        () -> assertThat(m.getId()).isEqualTo(2),
                        () -> assertThat(m.getOtherId()).isEqualTo(1),
                        () -> assertThat(m.getSequence()).isEqualTo(3)
                ));
    }
}
