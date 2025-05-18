package utils;

import algorithm.model.Ue2VmMapping;
import model.PM;
import model.UE;
import model.VM;
import service.MecSystemService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TestSystemSetup {

    private static MecSystemService mecService;

    public static void setupTestInstance(long seed) {
        mecService = MecSystemService.getInstance(0.5);
        mecService.setOffloadingDurationTime(0.03);

        int totalPms = 30;
        int totalVms = 30;
        int totalUes = 30;

        Random rand = new Random(seed);
        for (int i = 0; i < totalPms; i++) {
            int cores = rand.nextInt(1, 32);
            int gbs = cores * rand.nextInt(2, 4);
            double opsPerSec = rand.nextDouble(1.0, 9.0) * Math.pow(10, 9);
            mecService.addPM(new PM(cores, gbs, opsPerSec));
        }

        for (int i = 0; i < totalVms; i++) {
            int cores = rand.nextInt(1, 16);
            int gbs = cores * rand.nextInt(1, 4);
            double energyConsumption = rand.nextDouble(1.0, 1.3) * Math.pow(10, -8);
            mecService.addVM(new VM(cores, gbs, energyConsumption));
        }

        for (int i = 0; i < totalUes; i++) {
            int cores = rand.nextInt(1, 8);
            int gbs = rand.nextInt(1, 8);
            int taskSize = rand.nextInt(10, 40);
            double transmitPower = rand.nextDouble(0.01, 0.1);
            mecService.addUE(new UE(cores, gbs, taskSize, transmitPower));
        }

        // setup the (random) mapping between UEs and VMs
        setupUe2VmMappings(seed);
    }

    public static void setupUe2VmMappings(long seed) {

        // set seed for random number generator
        Random rand = new Random(seed);

        int totalVms = mecService.getNumberOfVMs();
        List<Integer> testedVmIds = new ArrayList<>(totalVms);

        for (UE ue : mecService.getUEs()) {
            int cores = ue.getRequiredOffloadedCores();
            int memory = ue.getRequiredOffloadedMemoryGB();

            boolean vmFound = false;
            VM vm = null;
            testedVmIds.clear();
            do {
                if (testedVmIds.size() == totalVms) {
                    break;
                }

                int vmId = rand.nextInt(totalVms);
                if (testedVmIds.contains(vmId)) {
                    continue;
                }
                vm = mecService.getVM(vmId);
                if (mecService.checkEnoughVmResources(vmId, cores, memory)) {
                    vmFound = true;
                }
                testedVmIds.add(vmId);
            } while (!vmFound);

            if (vmFound) {
                mecService.addUe2VmMapping(new Ue2VmMapping(ue.getId(), vm.getId(), cores, memory));
            }
        }
    }
}