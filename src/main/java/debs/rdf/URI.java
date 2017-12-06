package debs.rdf;

public class URI {
    final String namespace;
    final String className;
    String litValue;

    public URI(String ns, String cls) {
        namespace = ns;
        className = cls;
    }

    public URI(String ns, String cls, String val) {
        namespace = ns;
        className = cls;
        litValue = val;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getClassName() {
        return className;
    }

    public String getLitValue() {
        return litValue;
    }

    @Override
    public String toString() {
        String infoStr = String.format("NS: %s; Class: %s", namespace, className);
        if (litValue != null) {
            infoStr = infoStr + String.format("; Lit Value: %s", litValue);
        }
        return infoStr;
    }
}
