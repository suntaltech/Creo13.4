package p1.controlled.step13;

import java.util.*;

/** Pure JSON and evidence bookkeeping; no SDK/runtime model dependencies. */
public final class AuditSupport {
    private AuditSupport() { }
    public static Map<String,Object> map(Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Odd key/value count");
        Map<String,Object> value = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) value.put((String)pairs[i], pairs[i + 1]);
        return value;
    }
    public static Object known(Object value) { return value == null ? "UNKNOWN" : value; }
    public static Double positive(Double value) { return value != null && Double.isFinite(value) && value > 0 ? value : null; }
    public static Map<String,Object> coverage(Integer total, int completed) {
        if (completed < 0 || (total != null && (total < 0 || completed > total))) throw new IllegalArgumentException("Invalid coverage counts");
        return map("denominator", known(total), "completed", completed,
                "ratio", total == null || total == 0 ? null : (double)completed / total,
                "status", total == null ? "DENOMINATOR_UNKNOWN" : total == 0 ? "EMPTY_NOT_PERCENT_COMPLETE" : completed == total ? "ALL_ENUMERATED_ENTRIES" : "PARTIAL");
    }
    public static String completion(boolean eligible, boolean changed, boolean fatal, boolean requiredReadsComplete) {
        return changed ? "OBSERVED_IDENTITY_OR_DIRTY_CHANGE" : fatal ? "FAILED" : !eligible ? "BLOCKED"
                : requiredReadsComplete ? "COMPLETED_READS_ONLY" : "PARTIAL";
    }
    public static boolean stable(Object before, Object after) { return before != null && after != null && before.equals(after); }
    public static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return quote((String)value);
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Number) {
            if (value instanceof Double && !Double.isFinite((Double)value) || value instanceof Float && !Float.isFinite((Float)value)) throw new IllegalArgumentException("Non-finite JSON number");
            return value.toString();
        }
        if (value instanceof Map) {
            List<String> fields = new ArrayList<>();
            for (Map.Entry<?,?> entry : ((Map<?,?>)value).entrySet()) fields.add(quote(String.valueOf(entry.getKey())) + ":" + json(entry.getValue()));
            return "{" + String.join(",", fields) + "}";
        }
        if (value instanceof Iterable) {
            List<String> items = new ArrayList<>();
            for (Object item : (Iterable<?>)value) items.add(json(item));
            return "[" + String.join(",", items) + "]";
        }
        throw new IllegalArgumentException("Unsupported JSON type: " + value.getClass().getName());
    }
    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else if (c < 0x20 || Character.isSurrogate(c)) out.append(String.format(Locale.ROOT, "\\u%04x", (int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
