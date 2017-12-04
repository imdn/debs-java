package debs;

import debs.rdf.Parser;
import debs.rdf.Triple;
import debs.utils.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TaskProcessor implements MachineEventListener {
    private static Metadata metadata = new Metadata();
    private static EventCollection events = new EventCollection();
    private Parser parser = new Parser();

    private static final Logger logger = LoggerFactory.getLogger(TaskProcessor.class);

    public TaskProcessor(String metadataFilename) {
        deSerializeMetadata(metadataFilename);
        parser.addEventListener(this);
    }

    public void processMessage(byte[] message) {
        String line = new String(message, StandardCharsets.UTF_8);
        Triple triple = parser.getTriples(line.trim());
        //metadata.processMetadata(triple);
        parser.processObservations(triple, events, metadata);
    }

    @Override
    public void observationGroupStreamedIn(String obsGrpId) {
        logger.debug("Observation Group was streamed in: " + obsGrpId);
    }

    public void cleanUp() {
        // Termination message received at this point
        parser.processFinalObservationGroup();
    }

    public void serializeMetadata(String filename) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(metadata);
            oos.close();
            fos.close();
            logger.debug("Metadata written to: " + filename);
            metadata.printMetadata();
        } catch (IOException e) {
            logger.debug("IOException", e);
        }
    }

    private void deSerializeMetadata(String filename) {
        logger.debug("Deserializing metadata from: ", filename);
        try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            metadata = (Metadata) ois.readObject();
            fis.close();
            ois.close();
        } catch (ClassNotFoundException e) {
            logger.debug("Class not found", e);
        } catch (IOException e) {
            logger.debug("IO Error", e);
        }
    }

    public void printMetadata() {
        metadata.printMetadata();
    }
}
