package org.aksw.agdistis.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.aksw.agdistis.datatypes.Candidate;
import org.springframework.jdbc.core.RowMapper;

public class CandidateRowMapper implements RowMapper<Candidate> {

    @Override
    public Candidate mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Candidate(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
    }

}
