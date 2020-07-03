package com.kappadrive.dao.test.repository;

import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.api.JdbcDao;
import com.kappadrive.dao.test.model.User;

@GenerateDao
public interface UserDao extends JdbcDao<User, Long> {
}
