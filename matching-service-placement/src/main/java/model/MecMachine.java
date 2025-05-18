package model;

import java.util.Objects;

public abstract class MecMachine extends MecElement{
    private final int id;
    private final int totCores;
    private final int totMemoryGB;


    public MecMachine(int id, int totCores, int totMemoryGB, String elementName) {
        super(elementName);
        this.id = id;
        this.totCores = totCores;
        this.totMemoryGB = totMemoryGB;
    }

    @Override
    public String toString() {
        return getShortName() + " " + getMachineDetails();
    }

    public String getShortName() {
        return super.getElementName() + "_" + getId();
    }

    public String getMachineDetails() {
        return "(cores=" + getTotCores() + ", GB=" + getTotMemoryGB() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, totCores, totMemoryGB);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MecMachine mc = (MecMachine) o;
        return id == mc.id && totCores == mc.totCores && totMemoryGB == mc.totMemoryGB;
    }

    public int getId() {
        return id;
    }

    public int getTotCores() {
        return totCores;
    }

    public int getTotMemoryGB() {
        return totMemoryGB;
    }
}
