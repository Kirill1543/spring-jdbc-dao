package com.kappadrive.dao.api;

import javax.annotation.Nonnull;

public interface JdbcDao<T> {

    @Nonnull
    Iterable<T> findAll();

    @Nonnull
    <S extends T> S insert(@Nonnull S entity);

    @Nonnull
    <S extends T> Iterable<S> insertAll(@Nonnull Iterable<? extends S> entities);

    void update(@Nonnull T entity);

    void updateAll(@Nonnull Iterable<? extends T> entities);

    void deleteAll();

}
