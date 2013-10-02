package org.aksw.agdistis.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.aksw.agdistis.datatypes.Voting;

public class VotingRowMapper implements ConfigurableRowMapper<Voting> {

    private int votingIdRowId;
    private int candidateIdRowId;
    private int candidateUrlRowId;
    private int candidateLabelRowId;
    private int usernameRowId;

    public VotingRowMapper(int votingIdRowId, int candidateIdRowId, int candidateUrlRowId, int candidateLabelRowId,
            int usernameRowId) {
        if ((votingIdRowId < 0) || (candidateIdRowId < 0) || (usernameRowId < 0)) {
            throw new IllegalArgumentException(
                    "At least the votingIdRowId, candidateIdRowId and usernameRowId have to be valid.");
        }
        this.votingIdRowId = votingIdRowId;
        this.candidateIdRowId = candidateIdRowId;
        this.candidateUrlRowId = candidateUrlRowId;
        this.candidateLabelRowId = candidateLabelRowId;
        this.usernameRowId = usernameRowId;
    }

    @Override
    public Voting mapRow(ResultSet rs, int rowNum) throws SQLException {
        Voting voting = new Voting(rs.getInt(votingIdRowId), rs.getInt(candidateIdRowId), rs.getString(usernameRowId));// rs.getString(labelRowId)
        if (candidateUrlRowId != NOT_IN_RESULT_SET) {
            voting.setUrl(rs.getString(candidateUrlRowId));
        }
        if (candidateLabelRowId != NOT_IN_RESULT_SET) {
            voting.setLabel(rs.getString(candidateLabelRowId));
        }
        return voting;
    }
}
