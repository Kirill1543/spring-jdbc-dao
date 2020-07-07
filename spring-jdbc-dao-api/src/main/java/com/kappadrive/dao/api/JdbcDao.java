package com.kappadrive.dao.api;

import javax.annotation.Nonnull;

public interface JdbcDao<T> {

    @Nonnull
    Iterable<T> findAll();

    @Nonnull
    <S extends T> S insert(@Nonnull S entity);

    void update(@Nonnull T entity);

    void deleteAll();

}
