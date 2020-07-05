package com.kappadrive.dao.test.model;

import com.kappadrive.dao.api.ColumnType;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Types;

@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @Column(name = "family_name")
    private String otherName;
    @ColumnType(Types.VARCHAR)
    private UserRole role;
}
