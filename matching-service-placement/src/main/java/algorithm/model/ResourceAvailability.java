package algorithm.model;

public class ResourceAvailability {
    private final int id;
    private final int totCores;
    private final int totMemory;
    private final int totAllocations;
    private int usedCores;
    private int usedMemory;
    private int usedAllocations;

    public ResourceAvailability(int id, int totCores, int totMemory, int totAllocations) {
        this.id = id;
        this.totCores = totCores;
        this.totMemory = totMemory;
        this.totAllocations = totAllocations;
        this.usedCores = 0;
        this.usedMemory = 0;
        this.usedAllocations = 0;
    }

    public int getId() {
        return id;
    }

    public int getTotCores() {
        return totCores;
    }

    public int getTotMemory() {
        return totMemory;
    }

    public void allocateResources(int cores, int memory) {
        allocateCores(cores);
        allocateMemory(memory);
        this.usedAllocations++;
    }

    public void releaseResources(int cores, int memory) {
        releaseCores(cores);
        releaseMemory(memory);
        this.usedAllocations--;
    }

    public void allocateCores(int cores) {
        if (this.usedCores + cores <= this.totCores) {
            this.usedCores += cores;
        }
    }

    public void releaseCores(int cores) {
        if (this.usedCores - cores >= 0) {
            this.usedCores -= cores;
        }
    }

    public void allocateMemory(int memory) {
        if (this.usedMemory + memory <= this.totMemory) {
            this.usedMemory += memory;
        }
    }

    public void releaseMemory(int memory) {
        if (this.usedMemory - memory >= 0) {
            this.usedMemory -= memory;
        }
    }

    public int getAvailableCores() {
        return this.totCores - this.usedCores;
    }

    public int getAvailableMemory() {
        return this.totMemory - this.usedMemory;
    }

    public int getUsedCores() {
        return this.usedCores;
    }

    public int getUsedMemory() {
        return this.usedMemory;
    }

    public int getUsedAllocations() {
        return usedAllocations;
    }

    public boolean areResourcesAvailable(int cores, int memory) {
        return this.usedCores + cores <= this.totCores && this.usedMemory + memory <= this.totMemory;
    }

    public boolean canPerformMatch(int cores, int memory) {
        return areResourcesAvailable(cores, memory) && this.usedAllocations < this.totAllocations;
    }
}
