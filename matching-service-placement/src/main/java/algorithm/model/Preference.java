package algorithm.model;

public class Preference {
    private final int proposer;
    private int receiver;
    private double preference;
    private final Ue2VmMapping ue2VmMapping;

    public Preference(int proposer, int receiver, double preference, Ue2VmMapping ue2VmMapping) {
        this.proposer = proposer;
        this.receiver = receiver;
        this.preference = preference;
        this.ue2VmMapping = ue2VmMapping;
    }

    public int getProposer() {
        return proposer;
    }

    public int getReceiver() {
        return receiver;
    }

    public void setReceiver(int receiver) {
        this.receiver = receiver;
    }

    public double getPreference() {
        return preference;
    }

    public void setPreference(double preference) {
        this.preference = preference;
    }

    public Ue2VmMapping getUe2VmMapping() {
        return ue2VmMapping;
    }

    @Override
    public String toString() {
        return "Preference{" +
                "proposer=" + proposer +
                ", receiver=" + receiver +
                ", preference=" + preference +
                '}';
    }
}