package model;

public abstract class MecElement {
    private final String elementName;

    public MecElement(String elementName){
        this.elementName = elementName;
    }

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object o);

    public String getElementName() {
        return elementName;
    }
}
