package com.kappadrive.dao.test.model;

import com.kappadrive.dao.api.Column;
import com.kappadrive.dao.api.GeneratedValue;
import com.kappadrive.dao.api.Id;
import com.kappadrive.dao.api.Table;
import lombok.Data;

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
    @Column(type = Types.VARCHAR)
    private UserRole role;
}
