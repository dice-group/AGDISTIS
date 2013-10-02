package org.aksw.agdistis.db;

import org.springframework.jdbc.core.RowMapper;

public interface ConfigurableRowMapper<T> extends RowMapper<T> {

    public static final int NOT_IN_RESULT_SET = -1;

}
