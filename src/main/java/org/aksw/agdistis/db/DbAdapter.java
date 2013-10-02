package org.aksw.agdistis.db;

import java.util.ArrayList;
import java.util.List;

import org.aksw.agdistis.datatypes.Candidate;
import org.aksw.agdistis.datatypes.Label;
import org.aksw.agdistis.datatypes.Text;
import org.aksw.agdistis.datatypes.TextWithLabels;
import org.aksw.agdistis.datatypes.Voting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DbAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbAdapter.class);

    public static final int ID_NOT_FOUND = -1;
    public static final int NO_ENTITY = -1;
    public static final int UNKNOWN_CANDIDAET = -2;

    // private static final String SELECT_NEXT_TEXT_FOR_USER = "SELECT textsHavingCandidates.textID "
    // + "FROM textsHavingCandidates "
    // + "WHERE textsHavingCandidates.textID NOT IN ( "
    // + "               SELECT textHasLabel.textID "
    // + "               FROM voting, textHasLabel "
    // + "               WHERE voting.textHasLabelID=textHasLabel.textHasLabelID "
    // + "               AND voting.username = ?) "
    // + "AND ( textsHavingCandidates.textID IN ("
    // + "               SELECT text.textID "
    // + "               FROM voting, text, textHasLabel "
    // + "               WHERE voting.textHasLabelID=textHasLabel.textHasLabelID "
    // + "               AND textHasLabel.textID=text.textID"
    // + "               GROUP BY text.textID "
    // + "               HAVING count(DISTINCT voting.username) < 5)"
    // + "OR textsHavingCandidates.textID NOT IN ( "
    // + "               SELECT textHasLabel.textID "
    // + "               FROM voting JOIN textHasLabel USING (textHasLabelID)))"
    // + "LIMIT 1;";

    private static final String SELECT_NEXT_TEXT_FOR_USER = "SELECT text.textID "
            + "FROM text "
            + "WHERE text.textID NOT IN ( "
            + "               SELECT userSawText.textID "
            + "               FROM userSawText "
            + "               WHERE userSawText.username = ?) "
            + "AND ( text.textID IN ("
            + "               SELECT userSawText.textID "
            + "               FROM userSawText "
            + "               GROUP BY userSawText.textID "
            + "               HAVING count(DISTINCT userSawText.username) < 5)"
            + "OR text.textID NOT IN ( "
            + "               SELECT userSawText.textID "
            + "               FROM userSawText))"
            + "LIMIT 1;";

    private static final String SELECT_TEXT_WITH_TEXT_ID = "SELECT text.text " + "FROM text "
            + "WHERE text.textID = ?;";

    private static final String SELECT_LABELS_OF_TEXT = "SELECT label.labelID, textHasLabel.textHasLabelID, textHasLabel.start, textHasLabel.end, label.label "
            + "FROM label JOIN textHasLabel USING (labelID) "
            + "WHERE textHasLabel.textID = ? "
            + "GROUP BY textHasLabel.end "
            + "HAVING min(textHasLabel.start) OR textHasLabel.start=0 "
            + "ORDER BY textHasLabel.end DESC, textHasLabel.start;";
    private static final String SELECT_ALL_LABELS = "SELECT label.labelID, label.label FROM label";

    private static final String SELECT_CANDIDATES_FOR_LABEL = "SELECT candidate.candidateID, candidate.url, candidate.label, candidate.abstract "
            + "     FROM candidate, labelHasCandidate "
            + "     WHERE candidate.candidateID = labelHasCandidate.candidateID AND labelHasCandidate.labelID = ?;";

    private static final String SELECT_HIGHEST_TEXT_ID = "SELECT max(text.textID) FROM text";

    private static final String SELECT_ID_OF_LABEL = "SELECT label.labelID FROM label WHERE label.label=?";
    private static final String SELECT_ID_OF_CANDIDATE = "SELECT candidate.candidateID FROM candidate WHERE candidate.url=?";
    private static final String SELECT_EXISTENZ_OF_LABEL_HAS_CANDIDATE_RELATION = "SELECT count(*) FROM labelHasCandidate WHERE labelHasCandidate.labelID=? AND labelHasCandidate.candidateID=?";
    private static final String SELECT_IDS_OF_VOTED_TEXTS = "SELECT ust.textID FROM userSawText ust GROUP BY ust.textID HAVING Count(ust.username) >= ?";

    private static final String SELECT_POSITIV_VOTED_LABELS_IN_TEXT = "SELECT v.votingID, c.candidateID, c.url, c.label, v.username FROM voting v, candidate c WHERE v.textHasLabelID=? AND v.candidateID=c.candidateID";
    private static final String SELECT_VOTED_LABELS_WHICH_ARE_NO_ENTITIES = "SELECT v.votingID, v.candidateID, v.username FROM voting v WHERE v.textHasLabelID=? AND v.candidateID="
            + Candidate.NO_ENTITY_CANDIDATE_ID;
    private static final String SELECT_VOTED_LABELS_WITH_UNKNOWN_CANDIDATE_IN_TEXT = "SELECT v.votingID, v.candidateID, a.anotherEntity, a.anotherEntity, v.username FROM voting v, anotherEntity a WHERE v.textHasLabelID=? AND v.candidateID="
            + Candidate.OTHER_ENTITY_CANDIDATE_ID + " AND v.textHasLabelID=a.textHasLabelID AND v.username=a.username";
    private static final String SELECT_NOT_IDENTIFIED_NES_IN_TEXT = "SELECT label, start, end FROM notIdentifiedNE WHERE textID=?";

    private static final String INSERT_NOT_IDENTIFIED_NE = "INSERT INTO notIdentifiedNE (textID, label, start, end) VALUES (?, ?, ?, ? )";
    private static final String INSERT_VOTING = "INSERT INTO voting (textHasLabelID, username, candidateID) VALUES (?, ?, ?)";
    private static final String INSERT_ANOTHER_ENTITY = "INSERT INTO anotherEntity (textHasLabelID, username, anotherEntity) VALUES (?, ?, ?)";
    // private static final String INSERT_VOTING = "INSERT INTO voting (textID, username, labelID, uri) " +
    // "SELECT textHasLabel.textHasLabelID, ?, ? " +
    // "FROM textHasLabel " +
    // "WHERE textHasLabel.textID=? " +
    // "AND textHasLabel.labelID=";
    private static final String INSERT_TEXT = "INSERT INTO text (textID, text) VALUES (?, ?)";
    private static final String INSERT_LABEL = "INSERT INTO label (label) VALUES (?)";
    private static final String INSERT_TEXT_HAS_LABEL = "INSERT INTO textHasLabel (textID, labelID, start, end) VALUES (?, ?, ?, ?)";
    private static final String INSERT_CANDIDATE = "INSERT INTO candidate (url, label, abstract) VALUES (?, ?, ?)";
    private static final String INSERT_LABEL_HAS_CANDIDATE = "INSERT INTO labelHasCandidate (labelID, candidateID) VALUES (?, ?)";
    private static final String INSERT_USER_SAW_TEXT = "INSERT INTO userSawText (textID, username) VALUES (?, ?)";

    private JdbcTemplate jdbctemplate;

    public DbAdapter(ComboPooledDataSource datasource) {
        this.jdbctemplate = new JdbcTemplate(datasource);
    }

    protected boolean doesLabelAlreadyHasCandidate(Integer labelId, Integer candidateId) {
        List<Integer> candidateIds = jdbctemplate.query(SELECT_EXISTENZ_OF_LABEL_HAS_CANDIDATE_RELATION, new Object[] {
                labelId, candidateId }, new IntegerRowMapper());
        if (candidateIds.size() > 0) {
            return candidateIds.get(0) > 0;
        } else {
            return false;
        }
    }

    public List<Label> getAllLabels() {
        // label.labelID, label.label
        return jdbctemplate.query(SELECT_ALL_LABELS, new LabelRowMapper(2, 1, LabelRowMapper.NOT_IN_RESULT_SET,
                LabelRowMapper.NOT_IN_RESULT_SET, LabelRowMapper.NOT_IN_RESULT_SET));
    }

    public List<Candidate> getCandidatesForLabel(Integer labelId) {
        return jdbctemplate.query(SELECT_CANDIDATES_FOR_LABEL, new Object[] { labelId }, new CandidateRowMapper());
    }

    public int getIdOfCandidate(String url) {
        List<Integer> candidateIds = jdbctemplate.query(SELECT_ID_OF_CANDIDATE, new Object[] { url },
                new IntegerRowMapper());
        if (candidateIds.size() > 0) {
            return candidateIds.get(0);
        } else {
            return ID_NOT_FOUND;
        }
    }

    public int getIdOfCandidateAndInsertIfNotExists(Candidate candidate) {
        int candidateId = getIdOfCandidate(candidate.getUrl());
        if (candidateId == ID_NOT_FOUND) {
            insertCandidate(candidate);
            candidateId = getIdOfCandidate(candidate.getUrl());
        }
        return candidateId;
    }

    public int getIdOfLabel(String label) {
        List<Integer> labelIds = jdbctemplate.query(SELECT_ID_OF_LABEL, new Object[] { label }, new IntegerRowMapper());
        if (labelIds.size() > 0) {
            return labelIds.get(0);
        } else {
            return ID_NOT_FOUND;
        }
    }

    public int getIdOfLabelAndInsertIfNotExists(String label) {
        int labelId = getIdOfLabel(label);
        if (labelId == ID_NOT_FOUND) {
            insertLabel(label);
            labelId = getIdOfLabel(label);
        }
        return labelId;
    }

    public List<Integer> getIdsOfVotedTexts() {
        return getIdsOfVotedTexts(1);
    }

    public List<Integer> getIdsOfVotedTexts(Integer minVotingCount) {
        return jdbctemplate.query(SELECT_IDS_OF_VOTED_TEXTS, new Object[] { minVotingCount }, new IntegerRowMapper());
    }

    public List<Label> getLabelsForText(Integer textId) {
        // label.labelID, textHasLabel.textHasLabelID, textHasLabel.start, textHasLabel.end, label.label
        return jdbctemplate.query(SELECT_LABELS_OF_TEXT, new Object[] { textId }, new LabelRowMapper(5, 1, 3, 4, 2));
    }

    private int getHighestTextId() {
        List<Integer> textIds = jdbctemplate.query(SELECT_HIGHEST_TEXT_ID, new IntegerRowMapper());
        if (textIds.size() > 0) {
            return textIds.get(0);
        } else {
            return ID_NOT_FOUND;
        }
    }

    public List<Label> getNotIdentifiedEntities(int textId) {
        return jdbctemplate.query(SELECT_NOT_IDENTIFIED_NES_IN_TEXT, new Object[] { textId }, new LabelRowMapper(1,
                LabelRowMapper.NOT_IN_RESULT_SET, 2, 3, LabelRowMapper.NOT_IN_RESULT_SET));
    }

    public TextWithLabels getTextForUser(String userName) {
        Integer textId = getTextIdForUser(userName);
        if (textId.intValue() == ID_NOT_FOUND) {
            return null;
        }
        Text text = getTextWithId(textId);
        if (text == null) {
            return null;
        }
        List<Label> labels = getLabelsForText(textId);
        for (Label label : labels) {
            label.setCandidates(getCandidatesForLabel(label.getLabelId()));
        }
        return new TextWithLabels(text, labels);
    }

    public Integer getTextIdForUser(String userName) {
        List<Integer> textIds = jdbctemplate.query(SELECT_NEXT_TEXT_FOR_USER, new Object[] { userName },
                new IntegerRowMapper());
        if (textIds.size() > 0) {
            return textIds.get(0);
        } else {
            return ID_NOT_FOUND;
        }
    }

    public Text getTextWithId(Integer textId) {
        List<String> texts = jdbctemplate.query(SELECT_TEXT_WITH_TEXT_ID, new Object[] { textId },
                new StringRowMapper());
        if (texts.size() > 0) {
            return new Text(textId, texts.get(0));
        } else {
            return null;
        }
    }

    public TextWithLabels getTextWithVotings(Integer textId) {
        Text text = getTextWithId(textId);
        if (text == null) {
            return null;
        }
        List<Label> labels = getLabelsForText(textId);
        for (Label label : labels) {
            label.setCandidates(new ArrayList<Candidate>(getVotingsForLabel(label.getTextHasLabelId())));
        }

        return new TextWithLabels(text, labels);
    }

    public List<Voting> getVotingsForLabel(Integer textHasLabelId) {
        List<Voting> votings = jdbctemplate.query(SELECT_POSITIV_VOTED_LABELS_IN_TEXT, new Object[] { textHasLabelId },
                new VotingRowMapper(1, 2, 3, 4, 5));
        votings.addAll(jdbctemplate.query(SELECT_VOTED_LABELS_WHICH_ARE_NO_ENTITIES, new Object[] { textHasLabelId },
                new VotingRowMapper(1, 2, VotingRowMapper.NOT_IN_RESULT_SET, VotingRowMapper.NOT_IN_RESULT_SET, 3)));
        votings.addAll(jdbctemplate.query(SELECT_VOTED_LABELS_WITH_UNKNOWN_CANDIDATE_IN_TEXT,
                new Object[] { textHasLabelId },
                new VotingRowMapper(1, 2, 3, 4, 5)));
        return votings;
    }

    public void insertCandidate(Candidate candidate) {
        if (jdbctemplate.update(INSERT_CANDIDATE,
                new Object[] { candidate.getUrl(), candidate.getLabel(), candidate.getDescription() }) == 0) {
            LOGGER.error("Insertion of a new candidate had no effect.");
            return;
        }
    }

    /**
     * Inserts the given {@link Candidate} if it is not already in the database and inserts the relation to the label
     * with the given labelId.
     * 
     * @param labelId
     * @param candidate
     */
    public void insertCandidateOfLabel(Integer labelId, Candidate candidate) {
        insertLabelHasCandidate(labelId, getIdOfCandidateAndInsertIfNotExists(candidate));
    }

    /**
     * Inserts the given {@link Label} object, all its {@link Candidate}s and the relations between the {@link Label}
     * and the {@link Candidate}s, and returns the new id of the {@link Label}.
     * 
     * @param label
     */
    public int insertLabel(final Label label) {
        int labelId = getIdOfLabelAndInsertIfNotExists(label.getLabel());
        for (Candidate candidate : label.getCandidates()) {
            insertCandidateOfLabel(labelId, candidate);
        }
        return labelId;
    }

    public void insertLabel(String label) {
        if (jdbctemplate.update(INSERT_LABEL, new Object[] { label }) == 0) {
            LOGGER.error("Insertion of a new label had no effect.");
            return;
        }
    }

    /**
     * Inserts the relation of the label with the given labelId and the candidate with the given candidateId into the
     * database if it does not already exists.
     * 
     * @param labelId
     * @param candidateId
     */
    public void insertLabelHasCandidate(Integer labelId, Integer candidateId) {
        if (!doesLabelAlreadyHasCandidate(labelId, candidateId)) {
            if (jdbctemplate.update(INSERT_LABEL_HAS_CANDIDATE, new Object[] { labelId, candidateId }) == 0) {
                LOGGER.error("Insertion of a new labelHasCandidate relation had no effect.");
                return;
            }
        }
    }

    /**
     * Inserts the relation between the text with the given Id and the given label. If the label is not already in the
     * database it will be inserted first.
     * 
     * @param textId
     * @param label
     */
    public void insertLabelOfText(Label label, Integer textId) {
        int labelId = insertLabel(label);
        insertTextHasLabel(textId, labelId, label.getStart(), label.getEnd());
    }

    public void insertNotIdentifiedNE(Integer textId, String label, Integer start, Integer end) {
        if (jdbctemplate.update(INSERT_NOT_IDENTIFIED_NE, new Object[] { textId, label, start, end }) == 0) {
            LOGGER.error("Insertion of a new not identified NE had no effect.");
        }
    }

    /**
     * Inserts the given {@link Text} and returns its new Id. Note that the internal id of the {@link Text}-object is
     * not used or changed.
     * 
     * @param text
     */
    public int insertText(Text text) {
        int textId = getHighestTextId();
        if (textId == ID_NOT_FOUND) {
            textId = 0;
        }
        ++textId;
        insertText(text, textId);
        return textId;
    }

    /**
     * Inserts the given {@link Text} using the given textId. Note that the internal id of the {@link Text}-object is
     * not used or changed.
     * 
     * @param text
     * @param textId
     */
    public void insertText(Text text, int textId) {
        if (jdbctemplate.update(INSERT_TEXT, new Object[] { textId, text.getText() }) == 0) {
            LOGGER.error("Insertion of a new text had no effect.");
        }
    }

    /**
     * Inserts the relation between the text with the given Id and the given label. Note that the given label must have
     * its labelId set! If this is not the case use {@link #insertLabelOfText(Label, int)} instead.
     * 
     * @param textId
     * @param label
     */
    public void insertTextHasLabel(Integer textId, Label label) {
        insertTextHasLabel(textId, label.getLabelId(), label.getStart(), label.getEnd());
    }

    /**
     * Inserts the relation between the text with the given Id and the label with the given Id using the given start and
     * end positions.
     * 
     * @param textId
     * @param labelId
     * @param start
     * @param end
     */
    public void insertTextHasLabel(Integer textId, Integer labelId, Integer start, Integer end) {
        if (jdbctemplate.update(INSERT_TEXT_HAS_LABEL, new Object[] { textId, labelId, start, end }) == 0) {
            LOGGER.error("Insertion of a new textHasLabel relation had no effect.");
            return;
        }
    }

    /**
     * Inserts the given {@link TextWithLabels}, its {@link Label}s and their {@link Candidate}s, and returns the new Id
     * of the {@link TextWithLabels}.
     * 
     * @param text
     */
    public int insertTextWithLabels(final TextWithLabels text) {
        int textId = insertText(text);
        if (textId != ID_NOT_FOUND) {
            for (Label label : text.getLabels()) {
                insertLabelOfText(label, textId);
            }
        }
        return textId;
    }
    
    public int insertTextWithLabels(final TextWithLabels text, int textId) {
        insertText(text, textId);
        if (textId != ID_NOT_FOUND) {
            for (Label label : text.getLabels()) {
                insertLabelOfText(label, textId);
            }
        }
        return textId;
    }

    public void insertUserSawText(final String user, final Integer textId) {
        if (jdbctemplate.update(INSERT_USER_SAW_TEXT, new Object[] { textId, user }) == 0) {
            LOGGER.error("Insertion of a new textHasLabel relation had no effect.");
            return;
        }
    }

    public void insertVoting(Integer textHasLabelId, String username, Integer candidateId) {
        if (jdbctemplate.update(INSERT_VOTING, new Object[] { textHasLabelId, username, candidateId }) == 0) {
            LOGGER.error("Insertion of a new voting had no effect.");
        }
    }

    /**
     * Sets the Id of the the given {@link Candidate} to the Id inside the database. If this {@link Candidate} is not
     * already in the database it will be inserted.
     * 
     * @param candidate
     */
    public void setIdOfCandidateAndInsertIfNotExists(Candidate candidate) {
        candidate.setId(getIdOfCandidateAndInsertIfNotExists(candidate));
    }

    /**
     * Sets the Id of the the given {@link Label} to the Id inside the database. If this {@link Label} is not already in
     * the database it will be inserted.
     * 
     * @param label
     */
    public void setIdOfLabelAndInsertIfNotExists(Label label) {
        label.setLabelId(getIdOfLabelAndInsertIfNotExists(label.getLabel()));
    }

    public void insertAnotherEntity(Integer textHasLabelId, String userName, String anotherEntity) {
        if (jdbctemplate.update(INSERT_ANOTHER_ENTITY, new Object[] { textHasLabelId, userName, anotherEntity }) == 0) {
            LOGGER.error("Insertion of a new voting had no effect.");
        }

    }
}
