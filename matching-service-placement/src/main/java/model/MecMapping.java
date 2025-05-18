package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MecMapping {
    private static MecMapping instance = null;
    private int totalVms;
    private int totalPms;
    private final ArrayList<ArrayList<Integer>> vm2PmPlacement; // vm rows, pm columns
    private final ArrayList<ArrayList<Integer>> vmGb2PmPlacement; // vm rows, pm columns
    private final ArrayList<ArrayList<Integer>> vmCores2PmPlacement; // vm rows, pm columns

    private MecMapping(int totalVms, int totalPms) {
        this.totalVms = totalVms;
        this.totalPms = totalPms;
        vm2PmPlacement = new ArrayList<>();
        vmGb2PmPlacement = new ArrayList<>();
        vmCores2PmPlacement = new ArrayList<>();

        for (int i = 0; i < totalVms; i++) {
            vm2PmPlacement.add(new ArrayList<>(Collections.nCopies(totalPms, 0)));
            vmGb2PmPlacement.add(new ArrayList<>(Collections.nCopies(totalPms, 0)));
            vmCores2PmPlacement.add(new ArrayList<>(Collections.nCopies(totalPms, 0)));
        }
    }

    private MecMapping() {
        this(0, 0);
    }

    public static MecMapping getInstance() {
        if (instance == null) {
            instance = new MecMapping();
        }
        return instance;
    }


    public ArrayList<ArrayList<Integer>> getVm2PmPlacement() {
        return vm2PmPlacement;
    }

    public ArrayList<ArrayList<Integer>> getVmGb2PmPlacement() {
        return vmGb2PmPlacement;
    }

    public ArrayList<ArrayList<Integer>> getVmCores2PmPlacement() {
        return vmCores2PmPlacement;
    }

    public void setVmPlacement(int vmId, int pmId) {
        if (vmId >= 0 && vmId < totalVms && pmId >= 0 && pmId < totalPms)
            vm2PmPlacement.get(vmId).set(pmId, 1);
    }

    public void removeVmPlacement(int vmId, int pmId) {
        if (vmId >= 0 && vmId < totalVms && pmId >= 0 && pmId < totalPms)
            vm2PmPlacement.get(vmId).set(pmId, 0);
    }

    public void setVmCores2Pm(int vmId, int pmId, int vmCores) {
        vmCores2PmPlacement.get(vmId).set(pmId, vmCores);
        setVmPlacement(vmId, pmId);
    }

    public void setVmGb2Pm(int vmId, int pmId, int vmGb) {
        vmGb2PmPlacement.get(vmId).set(pmId, vmGb);
        setVmPlacement(vmId, pmId);
    }

    public ArrayList<Integer> getPmsHostingVm(int vmId) {
        return (ArrayList<Integer>) IntStream.range(0, vm2PmPlacement.get(vmId).size())
                .filter(i -> vm2PmPlacement.get(vmId).get(i) == 1)
                .boxed()
                .collect(Collectors.toList());

    }

    public ArrayList<Integer> getVmsHostedByPm(int pmId) {
        return (ArrayList<Integer>) IntStream.range(0, vm2PmPlacement.size())
                .filter(i -> vm2PmPlacement.stream().map(row -> row.get(pmId)).toList().get(i) == 1)
                .boxed()
                .collect(Collectors.toList());
    }

    public void addVm() {
        vm2PmPlacement.add(new ArrayList<>(Collections.nCopies(totalPms, 0)));
        vmGb2PmPlacement.add(new ArrayList<>(Collections.nCopies(totalPms, 0)));
        vmCores2PmPlacement.add(new ArrayList<>(Collections.nCopies(totalPms, 0)));
        this.totalVms++;
    }

    public void removeVm(int vmId) {
        vm2PmPlacement.remove(vmId);
        vmGb2PmPlacement.remove(vmId);
        vmCores2PmPlacement.remove(vmId);
        this.totalVms--;
    }

    public void addPm() {
        for (ArrayList<Integer> vmPlacement : vm2PmPlacement) {
            vmPlacement.add(0);
        }
        for (ArrayList<Integer> vmPlacement : vmGb2PmPlacement) {
            vmPlacement.add(0);
        }
        for (ArrayList<Integer> vmPlacement : vmCores2PmPlacement) {
            vmPlacement.add(0);
        }

        this.totalPms++;
    }

    public void removePm(int pmId) {
        for (ArrayList<Integer> vmPlacement : vm2PmPlacement) {
            vmPlacement.remove(pmId);
        }
        for (ArrayList<Integer> vmPlacement : vmGb2PmPlacement) {
            vmPlacement.remove(pmId);
        }
        for (ArrayList<Integer> vmPlacement : vmCores2PmPlacement) {
            vmPlacement.remove(pmId);
        }

        this.totalPms--;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MecMapping mecMapping = (MecMapping) obj;
        return vm2PmPlacement.equals(mecMapping.vm2PmPlacement) &&
                vmGb2PmPlacement.equals(mecMapping.vmGb2PmPlacement) &&
                vmCores2PmPlacement.equals(mecMapping.vmCores2PmPlacement);
    }

    public void resetMapping() {
        vm2PmPlacement.forEach(row -> Collections.fill(row, 0));
        vmGb2PmPlacement.forEach(row -> Collections.fill(row, 0));
        vmCores2PmPlacement.forEach(row -> Collections.fill(row, 0));
    }

    public void resetSystem() {
        vm2PmPlacement.clear();
        vmGb2PmPlacement.clear();
        vmCores2PmPlacement.clear();
        totalVms = 0;
        totalPms = 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Display header
        sb.append("=== VM to PM Resource Placement ===\n");
        sb.append("  (Rows: VMs, Columns: PMs)\n");

        // Core Allocation
        sb.append("\nAllocated Cores:\n");
        getResourceAllocationTable(sb, vmCores2PmPlacement);

        // GB Allocation
        sb.append("\nAllocated Memory (GB):\n");
        getResourceAllocationTable(sb, vmGb2PmPlacement);

        // PM Summary
        sb.append("\n=== PM Summary (Total Resources Allocated) ===\n");
        sb.append("PM   |   Total Cores   |   Total GB\n");
        sb.append("------------------------------------\n");
        for (int j = 0; j < vmGb2PmPlacement.get(0).size(); j++) {
            int totalGb = 0;
            int totalCores = 0;

            for (int i = 0; i < vmGb2PmPlacement.size(); i++) {
                totalCores += vmCores2PmPlacement.get(i).get(j);
                totalGb += vmGb2PmPlacement.get(i).get(j);
            }

            sb.append(String.format("PM %-2d |     %-8d |      %-8d\n", j, totalCores, totalGb));
        }

        return sb.toString();
    }

    private void getResourceAllocationTable(StringBuilder sb, ArrayList<ArrayList<Integer>> resourcePlacement) {
        List<Integer> totalResources = new ArrayList<>(Collections.nCopies(resourcePlacement.get(0).size(), 0));
        for (int i = 0; i < resourcePlacement.size(); i++) {
            sb.append("VM ").append(i).append(" | ");
            int total = 0;
            for (int j = 0; j < resourcePlacement.get(i).size(); j++) {
                int usedResources = resourcePlacement.get(i).get(j);
                sb.append(String.format("%4d", usedResources)).append(" ");
                total += usedResources;
                totalResources.set(j, totalResources.get(j) + usedResources);
            }
            sb.append(" | Tot: ").append(total);
            sb.append("\n");
        }
        sb.append("------------------------------------\nTot: | ");
        totalResources.forEach(r -> sb.append(String.format("%4d", r)).append(" "));
        sb.append("\n");
    }
}