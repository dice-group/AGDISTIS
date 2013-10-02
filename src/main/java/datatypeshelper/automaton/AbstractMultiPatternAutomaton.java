package datatypeshelper.automaton;

public abstract class AbstractMultiPatternAutomaton implements MultiPatternAutomaton {

    protected AutomatonCallback callback;

    public AbstractMultiPatternAutomaton(AutomatonCallback callback) {
        this.callback = callback;
    }

    public AutomatonCallback getCallback() {
        return callback;
    }

    public void setCallback(AutomatonCallback callback) {
        this.callback = callback;
    }
}
