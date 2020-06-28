package com.kappadrive.dao.api;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface JdbcDao<T, ID> {

    @Nonnull
    Iterable<T> findAll();

    @Nonnull
    Optional<T> findById(@Nonnull ID id);

    @Nonnull
    <S extends T> S insert(@Nonnull S entity);

    @Nonnull
    <S extends T> Iterable<S> insertAll(@Nonnull Iterable<? extends S> entities);

    void update(@Nonnull T entity);

    void updateAll(@Nonnull Iterable<? extends T> entities);

    void delete(@Nonnull ID id);

    void deleteAllById(@Nonnull Iterable<? extends ID> ids);

    void deleteAll();

    boolean exists(@Nonnull ID id);
}
