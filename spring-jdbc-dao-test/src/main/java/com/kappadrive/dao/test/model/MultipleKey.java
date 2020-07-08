package com.kappadrive.dao.test.model;

import com.kappadrive.dao.api.GeneratedValue;
import com.kappadrive.dao.api.Id;
import com.kappadrive.dao.api.Table;
import lombok.Data;

@Data
@Table(name = "multiple")
public class MultipleKey {

    @Id
    private Long id;
    @Id
    private Long otherId;
    @GeneratedValue
    private Long sequence;
}
