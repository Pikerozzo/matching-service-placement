package algorithm.model;

public record AlgorithmResults(String algorithmName, int totalAllocatedUes, int totalUes, int totalAllocatedVms,
                               int totalAllocatedPms, double totalEnergyConsumed) {

}