package org.aksw.agdistis.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.aksw.agdistis.datatypes.Label;

public class LabelRowMapper implements ConfigurableRowMapper<Label> {

    private int labelRowId;
    private int labelIdRowId;
    private int labelStartRowId;
    private int labelEndRowId;
    private int textHasLabelIdRowId;

    public LabelRowMapper(int labelRowId, int labelIdRowId, int labelStartRowId, int labelEndRowId,
            int textHasLabelIdRowId) {
        if (labelRowId < 0) {
            throw new IllegalArgumentException("At least the labelRowId has to be valid.");
        }
        this.labelRowId = labelRowId;
        this.labelIdRowId = labelIdRowId;
        this.labelStartRowId = labelStartRowId;
        this.labelEndRowId = labelEndRowId;
        this.textHasLabelIdRowId = textHasLabelIdRowId;
    }

    @Override
    public Label mapRow(ResultSet rs, int rowNum) throws SQLException {
        Label label = new Label(rs.getString(labelRowId));
        if (labelIdRowId != NOT_IN_RESULT_SET) {
            label.setLabelId(rs.getInt(labelIdRowId));
        }
        if (labelStartRowId != NOT_IN_RESULT_SET) {
            label.setStart(rs.getInt(labelStartRowId));
        }
        if (labelEndRowId != NOT_IN_RESULT_SET) {
            label.setEnd(rs.getInt(labelEndRowId));
        }
        if (textHasLabelIdRowId != NOT_IN_RESULT_SET) {
            label.setTextHasLabelId(rs.getInt(textHasLabelIdRowId));
        }
        return label;
    }
}
