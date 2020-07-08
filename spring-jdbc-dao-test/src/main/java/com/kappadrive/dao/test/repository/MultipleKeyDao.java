package com.kappadrive.dao.test.repository;

import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.test.model.MultipleKey;

import javax.annotation.Nonnull;
import java.util.Optional;

@GenerateDao(MultipleKey.class)
public interface MultipleKeyDao {

    @Nonnull
    MultipleKey insert(@Nonnull Long id, @Nonnull Long otherId);

    @Nonnull
    Optional<MultipleKey> find(@Nonnull Long id, @Nonnull Long otherId);
}
