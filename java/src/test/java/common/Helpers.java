package common;

import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;

public class Helpers {

    // If keys is null asserts that the maps are equal.
    // If keys is not null asserts that both maps have equal values for each of the keys
    public static void assertValuesOfKeysEqual(
            Map<String, Object> expectedFields,
            Map<String, Object> actualFields,
            List<String> keys) {

        Assertions.assertTrue(
                Helpers.areValuesOfKeysEqual(actualFields, expectedFields, keys));
    }

    // If keys is null returns true iff the maps are equal.
    // If keys is not null returns true iff both maps have equal values for each of the keys
    public static <K, V> boolean areValuesOfKeysEqual(
            Map<K, V> map1,
            Map<K, V> map2,
            List<K> keys) {

        if (keys == null) {
            return map1.equals(map2);
        }

        for (var key : keys) {
            if (!map1.get(key).equals(map2.get(key))) {
                return false;
            }
        }
        return true;
    }
}
