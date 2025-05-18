package algorithm.utils;

import algorithm.model.Preference;

import java.util.Comparator;

public class PreferenceComparator implements Comparator<Preference> {

    /**
     * Compares two Preference objects based on their preference values.
     * @param pref1 the first object to be compared.
     * @param pref2 the second object to be compared.
     * @return preferences in a descending order.
     */
    @Override
    public int compare(Preference pref1, Preference pref2) {
        return Double.compare(pref2.getPreference(), pref1.getPreference());
    }
}
