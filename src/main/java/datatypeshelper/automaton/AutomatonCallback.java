package datatypeshelper.automaton;

public interface AutomatonCallback {

    public void foundPattern(int patternId, int startPos, int length);
}
