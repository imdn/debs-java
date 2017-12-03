package debs.rdf;

public class Triple {
    private URI subject;
    private URI predicate;
    private URI object;

    Triple(URI[] uriArr) {
        subject = uriArr[0];
        predicate = uriArr[1];
        object = uriArr[2];
    }

    @Override
    public String toString() {
        String infoStr = String.format("Subject - %s\nPredicate - %s\nObject - %s",
                subject.toString(), predicate.toString(), object.toString());
        return infoStr;
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
