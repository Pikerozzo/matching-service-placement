package algorithm.model;

public class Ue2VmMapping{
    private int ueId;
    private int vmId;
    private int cores;
    private int memory;

    public Ue2VmMapping(int ueId, int vmId, int cores, int memory) {
        this.ueId = ueId;
        this.vmId = vmId;
        this.cores = cores;
        this.memory = memory;
    }

    public int getUeId() {
        return ueId;
    }

    public void setUeId(int ueId) {
        this.ueId = ueId;
    }

    public int getVmId() {
        return vmId;
    }

    public void setVmId(int vmId) {
        this.vmId = vmId;
    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }
}