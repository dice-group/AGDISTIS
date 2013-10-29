package org.aksw.agdistis.experiment;

import java.net.URL;
import java.util.HashSet;
import java.util.List;

import org.aksw.agdistis.datatypes.Label;
import org.aksw.agdistis.datatypes.TextWithLabels;
import org.aksw.agdistis.datatypes.Voting;
import org.aksw.agdistis.db.DbAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JudgeAgreement {
    private static Logger log = LoggerFactory.getLogger(JudgeAgreement.class);

    public static void main(String[] args) {
        DbAdapter dbAdapter = loadDBAdapter();
        int overallJudgedLabels = 0;
        double disagree = 0;
        double agree = 0;
        List<Integer> idsOfVotedText = dbAdapter.getIdsOfVotedTexts();
        // for each text, get votings from database
        for (int textId : idsOfVotedText) {
            log.info("TextId: " + textId);
            TextWithLabels text = dbAdapter.getTextWithVotings(textId);
            for (Label label : text.getLabels()) {
                overallJudgedLabels++;
                int textHasLabelId = label.getTextHasLabelId();
                List<Voting> votings = dbAdapter.getVotingsForLabel(textHasLabelId);

                if (votings.size() == 1) {
                    log.debug("\tSingle voting on label: " + label.getLabel() + "\tVoting: " + votings.get(0));
                } else {
                    HashSet<String> votes = new HashSet<String>();
                    for (Voting v : votings)
                    {
                        log.debug("\t\t" + v.getUrl());
                        if (v.getCandidateId() > 0) {
                            votes.add(v.getUrl());
                        } else {
                            votes.add(String.valueOf(v.getCandidateId()));
                        }
                    }

                    if (votes.size() == 1) {
                        agree++;
                    } else {
                        log.debug("\t\tDISAGREE");
                        disagree++;
                    }
                    log.debug("\t\t----");
                }
            }
        }
        log.info("Overall Judgements: " + overallJudgedLabels);
        log.info("Agreement: " + agree + "\t Disagreement: " + disagree);
        log.info("Percentage of Agreement: " + agree / (agree + disagree));
    }

    public static DbAdapter loadDBAdapter() {
        ClassLoader classLoader = TextDisambiguation.class.getClassLoader();
        URL resource = classLoader.getResource("applicationContext-jdbc.xml");
        String APPLICATION_CONTEXT_LOCATION = resource.toString().replace("file://", "");
        ApplicationContext context = new ClassPathXmlApplicationContext(APPLICATION_CONTEXT_LOCATION);
        DbAdapter dbAdapter = context.getBean(DbAdapter.class);
        return dbAdapter;
    }
}
