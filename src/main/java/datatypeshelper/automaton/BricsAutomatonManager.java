package datatypeshelper.automaton;

import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;

public class BricsAutomatonManager extends AbstractMultiPatternAutomaton {

    private RunAutomaton automata[];

    public BricsAutomatonManager(AutomatonCallback callback, String regexes[]) {
        super(callback);
        automata = new RunAutomaton[regexes.length];
        for (int i = 0; i < regexes.length; ++i) {
            automata[i] = new RunAutomaton((new RegExp(regexes[i])).toAutomaton());
        }
    }

    @Override
	public void parseText(String text) {
        int pos = 0;
        int textLength = text.length();
        if (textLength > 0) {
            int automatonId;
            int length = -1;
            while (pos < textLength) {
                automatonId = 0;
                while ((length < 0) && (automatonId < automata.length)) {
                    length = automata[automatonId].run(text, pos);
                    ++automatonId;
                }
                if (length < 0) {
                    ++pos;
                } else {
                    this.callback.foundPattern(automatonId - 1, pos, length);
                    pos += length;
                    length = -1;
                }
            }
        }
    }
}
