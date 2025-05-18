package algorithm;

import algorithm.model.AlgorithmResults;
import algorithm.model.Preference;
import service.EnergyConsumptionService;
import service.MecSystemService;

import java.util.ArrayList;

public abstract class MatchingAlg {
    protected int totalAllocatedUEs = 0;
    protected final MecSystemService mecService = MecSystemService.getInstance();
    protected final EnergyConsumptionService energyService = EnergyConsumptionService.getInstance();
    protected final ArrayList<Preference> finalMatches;

    MatchingAlg() {
        finalMatches = new ArrayList<>();
    }

    /**
     * Runs the algorithm.
     *
     * @param verbose whether to print verbose output
     * @return the results of the algorithm
     */
    public abstract AlgorithmResults run(boolean verbose);

    /**
     * Runs the algorithm with verbose output enabled.
     *
     * @return the results of the algorithm
     */
    public AlgorithmResults run(){
        return this.run(true);
    }


    /**
     * Allocates the VMs to the PMs based on the final matches.
     */
    protected void allocateMatchesToPMs(){
        for (Preference match : this.finalMatches) {
            int pmId = match.getReceiver();
            int vmId = match.getProposer();
            int cores = match.getUe2VmMapping().getCores();
            int memory = match.getUe2VmMapping().getMemory();

            mecService.addVMResourcesOnPm(vmId, pmId, cores, memory);
            this.totalAllocatedUEs++;
        }
    }

    /**
     * Returns the name of the algorithm.
     *
     * @return the name of the algorithm
     */
    public abstract String getName();

    /**
     * Prepares the results of the algorithm for final evaluations and analyses.
     *
     * @return the results of the algorithm
     */
    public AlgorithmResults prepareResults(){
        return new AlgorithmResults(this.getName(), totalAllocatedUEs, mecService.getNumberOfUEs(), mecService.getTotalAllocatedVms(), mecService.getTotalAllocatedPms(), energyService.getTotalEnergyConsumption());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}