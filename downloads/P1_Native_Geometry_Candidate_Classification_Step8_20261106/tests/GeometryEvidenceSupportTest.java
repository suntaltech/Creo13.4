import p1.readonly.geometry.step8.GeometryEvidenceSupport;
import java.util.*;

/** Pure logic tests only; no SDK/mock/native model objects. */
public final class GeometryEvidenceSupportTest {
    private static int tests;
    private static void check(boolean value) { tests++; if (!value) throw new AssertionError("check " + tests); }
    public static void main(String[] args) {
        Map<String,Object> unknown = GeometryEvidenceSupport.coverage(null, 0);
        check(unknown.get("ratio") == null && unknown.get("status").equals("DENOMINATOR_UNKNOWN"));
        Map<String,Object> empty = GeometryEvidenceSupport.coverage(0, 0);
        check(empty.get("ratio") == null && empty.get("status").equals("EMPTY_NOT_PERCENT_COMPLETE"));
        check(GeometryEvidenceSupport.coverage(4, 2).get("ratio").equals(0.5));
        check(GeometryEvidenceSupport.coverage(4, 4).get("status").equals("ALL_ENUMERATED_ENTRIES"));
        boolean rejected = false; try { GeometryEvidenceSupport.coverage(2, 3); } catch (IllegalArgumentException error) { rejected = true; } check(rejected);
        check(!GeometryEvidenceSupport.stable(null, null));
        check(!GeometryEvidenceSupport.stable("a", "b") && GeometryEvidenceSupport.stable("a", "a"));
        check(GeometryEvidenceSupport.positive(Double.NaN) == null && GeometryEvidenceSupport.positive(0.0) == null);
        check(GeometryEvidenceSupport.positive(1.5).equals(1.5));
        check(GeometryEvidenceSupport.known(Boolean.FALSE).equals(Boolean.FALSE));
        check(GeometryEvidenceSupport.completion(true, false, false, false).equals("PARTIAL"));
        check(GeometryEvidenceSupport.completion(true, false, false, true).equals("COMPLETED_READS_ONLY"));
        check(GeometryEvidenceSupport.completion(true, true, false, true).equals("OBSERVED_IDENTITY_OR_DIRTY_CHANGE"));
        check(GeometryEvidenceSupport.completion(true, false, true, true).equals("FAILED"));
        check(GeometryEvidenceSupport.completion(false, false, false, false).equals("BLOCKED"));
        rejected = false; try { GeometryEvidenceSupport.json(Double.POSITIVE_INFINITY); } catch (IllegalArgumentException error) { rejected = true; } check(rejected);
        check(GeometryEvidenceSupport.completion(false, false, true, false).equals("FAILED"));
        check(GeometryEvidenceSupport.completion(true, true, true, false).equals("OBSERVED_IDENTITY_OR_DIRTY_CHANGE"));
        String encoded = GeometryEvidenceSupport.json(GeometryEvidenceSupport.map("testKind", "SYNTHETIC_UNIT_TEST_NOT_RUNTIME_EVIDENCE", "text", "quote\" slash\\ newline\n tab\t 控制\u0001 emoji\uD83D\uDE00", "falseValue", false, "unknown", unknown, "items", Arrays.asList(1, null, true)));
        check(!encoded.contains("\n") && encoded.contains("\\n") && encoded.contains("\\u0001"));
        System.out.println(encoded);
        System.out.println("PURE_LOGIC_CHECKS_PASSED=" + tests);
    }
}
