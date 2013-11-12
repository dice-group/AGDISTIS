package datatypeshelper.io.xml.stream;

import java.io.IOException;
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReaderBasedTextMachine implements ReaderBasedTextMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractReaderBasedTextMachine.class);

    private boolean stopMachine;

    @Override
    public void analyze(Reader reader, ReaderBasedTextMachineObserver observer) {
        stopMachine = false;
        try {
            int i = reader.read();
            while (i > 0) {
                processNextChar((char) i, observer);
                if (stopMachine) {
                    return;
                }
                i = reader.read();
            }
        } catch (IOException e) {
            LOGGER.error("Got an Exception while reading from the given stream.", e);
            e.printStackTrace();
        }
    }

    protected abstract void processNextChar(char c, ReaderBasedTextMachineObserver observer);

    @Override
    public void stop() {
        stopMachine = true;
    }

}
