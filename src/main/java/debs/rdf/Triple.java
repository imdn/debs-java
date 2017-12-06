package debs.rdf;

public class Triple {
    private final URI subject;
    private final URI predicate;
    private final URI object;

    Triple(URI[] uriArr) {
        subject = uriArr[0];
        predicate = uriArr[1];
        object = uriArr[2];
    }

    @Override
    public String toString() {
        return String.format("Subject - %s\nPredicate - %s\nObject - %s",
                subject.toString(), predicate.toString(), object.toString());
    }

    public URI getSubject() {
        return subject;
    }

    public URI getPredicate() {
        return predicate;
    }

    public URI getObject() {
        return object;
    }
}
