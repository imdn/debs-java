package debs.rdf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import debs.utils.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parser {
    private static Metadata metadata = new Metadata();

    private static final Logger logger = LoggerFactory.getLogger(Parser.class);

    public Parser(String metadataFilename) {
        deSerializeMetadata(metadataFilename);
    }

    public void processMessage(byte[] message) {
        String line = new String(message, StandardCharsets.UTF_8);
        Triple triple = getTriples(line.trim());
        //logger.debug("Triple - " + triple.toString() + "\n");
        metadata.processMetadata(triple);
    }

    public Triple getTriples(String line) {
        String[] parts = line.split("\\s+");

        Pattern regex1 = Pattern.compile("^<([^>]+)#(.+)>$");
        Pattern regex2 = Pattern.compile("^\"(.+)\"\\^\\^<(.+)#(.+)>$");

        boolean parsed = false;
        boolean isValue = false;

        String subject, object, predicate;

        URI[] uris = new URI[3];

        //logger.debug(line);

        Matcher m;

        for (int i=0; i < parts.length; i++) {
            m = regex1.matcher(parts[i]);
            String currentToken = parts[i];
            if (m.matches()) {
                String namespace = m.group(1);
                String fragment = m.group(2);
                uris[i] = new URI(namespace, fragment);
            } else {
                m = regex2.matcher(currentToken);
                if (m.matches()) {
                    String literalValue = m.group(1);
                    String namespace = m.group(2);
                    String fragment = m.group(3);
                    uris[i] = new URI(namespace, fragment, literalValue);
                } else {
                    //logger.debug("Error! No regex match for - " + currentToken);
                }
            }
        }
        return new Triple(uris);
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

    public void deSerializeMetadata(String filename) {
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
