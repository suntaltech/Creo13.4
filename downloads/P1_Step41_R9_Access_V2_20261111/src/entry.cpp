// Required by the local SDK's guarded variadic message prototypes.
// Define before every SDK header; no build-script edit is required.
#ifndef PRO_USE_VAR_ARGS
#define PRO_USE_VAR_ARGS 1
#endif
// Step41: preserve validated commands; add default-body face count only.
// Entry ABI is unchanged; actual SDK documentation remains authoritative.
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <ProToolkit.h>
#include <ProToolkitDll.h>
#include <ProMessage.h>
#include <ProUICmd.h>
#include <ProMenuBar.h>
#include <ProMdl.h>
#include <ProSolid.h>
#include <ProSolidBody.h>
#include <ProArray.h>
#include <windows.h>
#include <cstdio>
#include <type_traits>
#include <cstdint>
#include <cinttypes>

static_assert(PRO_TK_NO_ERROR == 0, "Verify actual SDK success-code definition");

namespace {
volatile LONG trace_sequence = 0;
constexpr size_t model_name_units = sizeof(ProMdlName) / sizeof(wchar_t);
constexpr size_t model_name_utf8_capacity = model_name_units * 3 + 1;
constexpr size_t model_name_json_capacity = model_name_units * 6 + 3;
constexpr size_t model_facts_capacity = model_name_json_capacity + 1536;
constexpr size_t trace_line_capacity = model_facts_capacity + 512;

struct PreserveLastError {
    DWORD saved;
    PreserveLastError() noexcept : saved(GetLastError()) {}
    ~PreserveLastError() noexcept { SetLastError(saved); }
};

void trace_failure(const char* stage, DWORD error) noexcept
{
    char message[256] = {};
    const int count = std::snprintf(message, sizeof(message),
        "{\"tag\":\"P1_STEP35_DIAG_V1\",\"event\":\"TRACE_IO_FAILED\","
        "\"stage\":\"%s\",\"win32_error\":%lu}\n", stage,
        static_cast<unsigned long>(error));
    if (count > 0 && static_cast<size_t>(count) < sizeof(message)) {
        // Debugger output is a fallback signal, not a second persistent log.
        OutputDebugStringA(message);
    }
}

void trace_event(const char* event, const char* facts_json) noexcept
{
    PreserveLastError preserve;
    // First evaluated from a TOOLKIT callback, never from DllMain.
    static const ULONGLONG load_token = GetTickCount64();
    const LONG sequence = InterlockedIncrement(&trace_sequence);
    SYSTEMTIME utc = {};
    GetSystemTime(&utc);
    char line[trace_line_capacity] = {};
    const int count = std::snprintf(line, sizeof(line),
        "{\"schema\":1,\"tag\":\"P1_STEP35_DIAG_V1\","
        "\"utc\":\"%04u-%02u-%02uT%02u:%02u:%02u.%03uZ\","
        "\"pid\":%lu,\"tid\":%lu,\"load_token\":%llu,\"seq\":%ld,"
        "\"event\":\"%s\",\"facts\":%s}\n",
        static_cast<unsigned>(utc.wYear), static_cast<unsigned>(utc.wMonth),
        static_cast<unsigned>(utc.wDay), static_cast<unsigned>(utc.wHour),
        static_cast<unsigned>(utc.wMinute), static_cast<unsigned>(utc.wSecond),
        static_cast<unsigned>(utc.wMilliseconds),
        static_cast<unsigned long>(GetCurrentProcessId()),
        static_cast<unsigned long>(GetCurrentThreadId()),
        static_cast<unsigned long long>(load_token), static_cast<long>(sequence),
        event, facts_json);
    if (count <= 0 || static_cast<size_t>(count) >= sizeof(line)) {
        trace_failure("format", ERROR_INSUFFICIENT_BUFFER);
        return;
    }

    // Independent debugger channel; no debugger is attached by this code.
    OutputDebugStringA(line);

    wchar_t path[MAX_PATH] = {};
    const DWORD length = GetTempPathW(MAX_PATH, path);
    if (length == 0) {
        trace_failure("temp_path", GetLastError());
        return;
    }
    if (length >= MAX_PATH) {
        trace_failure("temp_path_length", ERROR_INSUFFICIENT_BUFFER);
        return;
    }
    char suffix[64] = {};
    const int suffix_length = std::snprintf(suffix, sizeof(suffix),
        "p1-step35-%lu.jsonl", static_cast<unsigned long>(GetCurrentProcessId()));
    if (suffix_length <= 0 || suffix_length >= 64 ||
        length + static_cast<DWORD>(suffix_length) >= MAX_PATH) {
        trace_failure("file_name", ERROR_INSUFFICIENT_BUFFER);
        return;
    }
    for (int i = 0; i <= suffix_length; ++i) {
        path[length + static_cast<DWORD>(i)] = static_cast<wchar_t>(suffix[i]);
    }
    // Append only; no directory creation, no truncation, no retry loop.
    const HANDLE file = CreateFileW(path, FILE_APPEND_DATA,
        FILE_SHARE_READ | FILE_SHARE_WRITE, nullptr, OPEN_ALWAYS,
        FILE_ATTRIBUTE_NORMAL, nullptr);
    if (file == INVALID_HANDLE_VALUE) {
        trace_failure("open", GetLastError());
        return;
    }
    DWORD written = 0;
    const BOOL ok = WriteFile(file, line, static_cast<DWORD>(count), &written, nullptr);
    const DWORD write_error = ok ? ERROR_WRITE_FAULT : GetLastError();
    if (!ok || written != static_cast<DWORD>(count)) {
        trace_failure("write", write_error);
    }
    if (!CloseHandle(file)) {
        trace_failure("close", GetLastError());
    }
}

// R4: observe registration identity only; do not query or execute commands.
// Local Creo 13.4 ProUICmd.h: typedef void *uiCmdCmdId.
static_assert(std::is_same<uiCmdCmdId, void*>::value,
    "Verify the supplied Creo 13.4 opaque command ID typedef");

void trace_registration_identity(const char* logical_command,
    uiCmdCmdId command_id, const char* sdk_action_name,
    const char* label_key, const char* help_key,
    const char* description_key, const wchar_t* message_file) noexcept
{
    // Call sites supply only fixed ASCII literals; no paths or model data.
    char ascii_file[64] = {};
    size_t i = 0;
    for (; message_file[i] != 0 && i + 1 < sizeof(ascii_file); ++i) {
        if (message_file[i] < 0x20 || message_file[i] > 0x7e) {
            trace_failure("registration_identity_filename", ERROR_INVALID_DATA);
            return;
        }
        ascii_file[i] = static_cast<char>(message_file[i]);
    }
    if (message_file[i] != 0) {
        trace_failure("registration_identity_filename_length", ERROR_INSUFFICIENT_BUFFER);
        return;
    }
    char facts[768] = {};
    const int count = std::snprintf(facts, sizeof(facts),
        "{\"revision\":\"UI_R4_R1_DIAG\",\"command\":\"%s\","
        "\"command_id\":\"0x%" PRIxPTR "\",\"sdk_action_name\":\"%s\","
        "\"label_key\":\"%s\",\"help_key\":\"%s\","
        "\"description_key\":\"%s\",\"message_file\":\"%s\"}",
        logical_command, reinterpret_cast<std::uintptr_t>(command_id), sdk_action_name,
        label_key, help_key, description_key, ascii_file);
    if (count <= 0 || static_cast<size_t>(count) >= sizeof(facts)) {
        trace_failure("registration_identity_format", ERROR_INSUFFICIENT_BUFFER);
        return;
    }
    trace_event("CMD_IDENTITY_BEFORE_DESIGNATE", facts);
}

// LOOKUP_FORMAT_HELPER_BEGIN
// Fixed ASCII command literals only; pointer values remain opaque identities.
bool format_lookup_facts(char* facts, size_t capacity,
    const char* logical_command, const char* sdk_name,
    const void* expected, const void* found, int status,
    int designate_status, bool success) noexcept
{
    char found_json[2 * sizeof(std::uintptr_t) + 5] = {};
    if (success) {
        const int n = std::snprintf(found_json, sizeof(found_json),
            "\"0x%" PRIxPTR "\"", reinterpret_cast<std::uintptr_t>(found));
        if (n <= 0 || static_cast<size_t>(n) >= sizeof(found_json)) return false;
    }
    const int n = std::snprintf(facts, capacity,
        "{\"revision\":\"UI_R9_ACCESS_V2\",\"phase\":\"after_designate_return\","
        "\"command\":\"%s\",\"sdk_action_name\":\"%s\",\"find_pro_error\":%d,"
        "\"designate_pro_error\":%d,\"action_add_id\":\"0x%" PRIxPTR "\","
        "\"found_id\":%s,\"found_id_null\":%s,\"id_match\":%s,"
        "\"UI_membership_read_back\":false}",
        logical_command, sdk_name, status, designate_status,
        reinterpret_cast<std::uintptr_t>(expected), success && found != nullptr ? found_json : "null",
        success ? (found == nullptr ? "true" : "false") : "null",
        success ? (found != nullptr && expected == found ? "true" : "false") : "null");
    return n > 0 && static_cast<size_t>(n) < capacity;
}
// LOOKUP_FORMAT_HELPER_END

void trace_command_lookup(const char* logical_command, char* sdk_name,
    uiCmdCmdId action_add_id, ProError designate_status) noexcept
{
    PreserveLastError preserve;
    char begin[192] = {};
    const int n = std::snprintf(begin, sizeof(begin),
        "{\"revision\":\"UI_R9_ACCESS_V2\",\"command\":\"%s\",\"sdk_action_name\":\"%s\"}",
        logical_command, sdk_name);
    if (n <= 0 || static_cast<size_t>(n) >= sizeof(begin)) {
        trace_failure("command_lookup_begin_format", ERROR_INSUFFICIENT_BUFFER);
        return;
    }
    trace_event("CMD_ID_FIND_PROBE_BEGIN", begin);
    uiCmdCmdId found = {};
    const ProError status = ProCmdCmdIdFind(sdk_name, &found);
    const bool success = status == PRO_TK_NO_ERROR;
    char facts[512] = {};
    if (!format_lookup_facts(facts, sizeof(facts), logical_command, sdk_name,
        action_add_id, success ? found : nullptr, static_cast<int>(status),
        static_cast<int>(designate_status), success)) {
        trace_failure("command_lookup_result_format", ERROR_INSUFFICIENT_BUFFER);
        return;
    }
    trace_event("CMD_ID_FIND_PROBE_RETURN", facts);
}

// The command callback never reads a model or dereferences command data.
int readonly_ping_action(uiCmdCmdId command, uiCmdValue* value, void* push_data)
{
    PreserveLastError preserve;
    (void)command;
    (void)value;
    (void)push_data;
    trace_event("READONLY_PING",
        "{\"command\":\"p1_readonly_ping\",\"result\":\"ok\",\"model_accessed\":false}");
    return 0;
}

uiCmdAccessState readonly_ping_access(uiCmdAccessMode mode)
{
    (void)mode;
    // Available with no model open; this callback performs no queries or logging.
    return ACCESS_AVAILABLE;
}

// ACCESS_OBSERVER_BEGIN
constexpr size_t access_sample_limit = 64;
struct AccessSample {
    int mode;
    int returned;
    ULONGLONG tick;
    volatile LONG ready; // 0: unpublished, 1: ready, 2: claimed by a flush.
};
struct AccessTrace {
    const char* command;
    const char* sdk_name;
    alignas(8) volatile LONG64 calls;
    alignas(8) volatile LONG64 modes[3]; // blind, messages allowed, other.
    AccessSample samples[access_sample_limit];
};
AccessTrace ping_access_trace = {"p1_readonly_ping", "p1_readonly_ping", 0, {}, {}};
AccessTrace model_access_trace = {"p1_readonly_model_info", "p1_readonly_model_info", 0, {}, {}};
AccessTrace body_access_trace = {"p1_readonly_body_face_count", "p1_readonly_fc", 0, {}, {}};

void record_access_observation(AccessTrace& state, uiCmdAccessMode mode,
    uiCmdAccessState returned) noexcept
{
    PreserveLastError preserve;
    // No file/debugger I/O, allocation, lock, retry loop, or model/SDK query.
    const LONG64 ticket = InterlockedIncrement64(&state.calls);
    const size_t mode_slot = mode == MODE_BLIND ? 0 :
        (mode == MODE_ERROR_MSG_ALLOWED ? 1 : 2);
    InterlockedIncrement64(&state.modes[mode_slot]);
    if (ticket <= 0 || ticket > static_cast<LONG64>(access_sample_limit)) return;
    AccessSample& sample = state.samples[static_cast<size_t>(ticket - 1)];
    sample.mode = static_cast<int>(mode);
    sample.returned = static_cast<int>(returned);
    sample.tick = GetTickCount64();
    // Publish after writing; the flush consumes only published entries.
    InterlockedExchange(&sample.ready, 1);
}

uiCmdAccessState ping_access_observed(uiCmdAccessMode mode)
{
    const uiCmdAccessState returned = readonly_ping_access(mode);
    record_access_observation(ping_access_trace, mode, returned);
    return returned;
}
uiCmdAccessState model_access_observed(uiCmdAccessMode mode)
{
    const uiCmdAccessState returned = readonly_ping_access(mode);
    record_access_observation(model_access_trace, mode, returned);
    return returned;
}
uiCmdAccessState body_access_observed(uiCmdAccessMode mode)
{
    const uiCmdAccessState returned = readonly_ping_access(mode);
    record_access_observation(body_access_trace, mode, returned);
    return returned;
}

void flush_one_access_trace(AccessTrace& state, const char* phase) noexcept
{
    PreserveLastError preserve;
    const LONG64 total = InterlockedCompareExchange64(&state.calls, 0, 0);
    LONG64 mode_totals[3] = {};
    for (size_t i = 0; i < 3; ++i)
        mode_totals[i] = InterlockedCompareExchange64(&state.modes[i], 0, 0);
    unsigned pending = 0;
    unsigned emitted = 0;
    for (size_t i = 0; i < access_sample_limit && static_cast<LONG64>(i) < total; ++i) {
        AccessSample& sample = state.samples[i];
        const LONG ready = InterlockedCompareExchange(&sample.ready, 2, 1);
        if (ready == 0) { ++pending; continue; }
        if (ready != 1) continue; // Already claimed; never emit it twice.
        char facts[512] = {};
        const int n = std::snprintf(facts, sizeof(facts),
            "{\"revision\":\"UI_R9_ACCESS_V2\",\"command\":\"%s\","
            "\"sdk_action_name\":\"%s\",\"flush_phase\":\"%s\","
            "\"call_ordinal\":%u,\"access_call_tick\":%llu,"
            "\"access_mode\":%d,\"access_returned\":%d,\"buffered\":true}",
            state.command, state.sdk_name, phase, static_cast<unsigned>(i + 1),
            static_cast<unsigned long long>(sample.tick), sample.mode, sample.returned);
        if (n > 0 && static_cast<size_t>(n) < sizeof(facts)) {
            trace_event("ACCESS_CALL_OBSERVED", facts);
            ++emitted;
        } else {
            trace_failure("access_sample_format", ERROR_INSUFFICIENT_BUFFER);
        }
    }
    char facts[512] = {};
    const int n = std::snprintf(facts, sizeof(facts),
        "{\"revision\":\"UI_R9_ACCESS_V2\",\"command\":\"%s\","
        "\"flush_phase\":\"%s\",\"total_calls\":%lld,"
        "\"mode_blind_calls\":%lld,\"mode_messages_allowed_calls\":%lld,"
        "\"mode_other_calls\":%lld,\"sample_limit\":%u,"
        "\"samples_emitted_in_flush\":%u,\"samples_unpublished_at_flush\":%u,"
        "\"calls_without_individual_sample\":%lld,\"snapshot_is_atomic\":false}",
        state.command, phase, static_cast<long long>(total),
        static_cast<long long>(mode_totals[0]), static_cast<long long>(mode_totals[1]),
        static_cast<long long>(mode_totals[2]), static_cast<unsigned>(access_sample_limit),
        emitted, pending, static_cast<long long>(total > static_cast<LONG64>(access_sample_limit)
            ? total - static_cast<LONG64>(access_sample_limit) : 0));
    if (n > 0 && static_cast<size_t>(n) < sizeof(facts)) trace_event("ACCESS_TRACE_SUMMARY", facts);
    else trace_failure("access_summary_format", ERROR_INSUFFICIENT_BUFFER);
}

void flush_access_observations(const char* phase) noexcept
{
    flush_one_access_trace(body_access_trace, phase);
    flush_one_access_trace(model_access_trace, phase);
    flush_one_access_trace(ping_access_trace, phase);
}
// ACCESS_OBSERVER_END

void trace_command_result(const char* event, ProError status) noexcept
{
    char facts[160] = {};
    const int count = std::snprintf(facts, sizeof(facts),
        "{\"command\":\"p1_readonly_ping\",\"pro_error\":%d}",
        static_cast<int>(status));
    trace_event(event, count > 0 && static_cast<size_t>(count) < sizeof(facts)
        ? facts : "null");
}

void register_readonly_ping()
{
    static char name[] = "p1_readonly_ping";
    static char label[] = "P1_READONLY_PING_LABEL";
    static char help[] = "P1_READONLY_PING_HELP";
    static char description[] = "P1_READONLY_PING_DESCRIPTION";
    static wchar_t message_file[] = L"p1_readonly_ping.txt";
    uiCmdCmdId command_id = {};
    // Real SDK typedefs check the callback signatures; no function-pointer casts.
    uiCmdCmdActFn action = &readonly_ping_action;
    uiCmdAccessFn access = &ping_access_observed;
    trace_event("CMD_REGISTER_BEGIN", "{\"command\":\"p1_readonly_ping\"}");
    const ProError add_status = ProCmdActionAdd(name, action, uiProeImmediate,
        access, PRO_B_TRUE, PRO_B_TRUE, &command_id);
    trace_command_result("CMD_ACTION_ADD_RETURN", add_status);
    if (add_status != PRO_TK_NO_ERROR) {
        // Do not reuse or overwrite a possibly stale, same-name command.
        return;
    }
    trace_registration_identity("p1_readonly_ping", command_id, name,
        label, help, description, message_file);
    const ProError designate_status = ProCmdDesignate(command_id, label,
        help, description, message_file);
    trace_command_result("CMD_DESIGNATE_RETURN", designate_status);
    trace_command_lookup("p1_readonly_ping", name, command_id, designate_status);
}
// PORTABLE_JSON_HELPER_BEGIN
// Input must be valid UTF-8; caller obtains it via strict Windows conversion.
// Never publish a truncated string as valid JSON.
bool quote_json_utf8(const char* input, char* output, size_t capacity) noexcept
{
    if (!input || !output || capacity < 3) {
        return false;
    }
    size_t used = 0;
    output[used++] = '"';
    const char hex[] = "0123456789abcdef";
    for (const unsigned char* p = reinterpret_cast<const unsigned char*>(input); *p; ++p) {
        char escaped[6] = {};
        size_t length = 1;
        escaped[0] = static_cast<char>(*p);
        if (*p == '"' || *p == '\\') {
            escaped[0] = '\\';
            escaped[1] = static_cast<char>(*p);
            length = 2;
        } else if (*p < 0x20) {
            escaped[0] = '\\'; escaped[1] = 'u';
            escaped[2] = '0'; escaped[3] = '0';
            escaped[4] = hex[*p >> 4]; escaped[5] = hex[*p & 0x0f];
            length = 6;
        }
        if (used + length + 2 > capacity) {
            output[0] = '\0';
            return false;
        }
        for (size_t i = 0; i < length; ++i) {
            output[used++] = escaped[i];
        }
    }
    output[used++] = '"';
    output[used] = '\0';
    return true;
}
// PORTABLE_JSON_HELPER_END

struct QueryStatus {
    bool called = false;
    ProError code = PRO_TK_NO_ERROR;
};

struct ModelInfoResult {
    const char* result = "query_failed";
    const char* no_active_model = "null";
    bool type_valid = false;
    ProMdlType type = {};
    bool name_valid = false;
    char name_json[model_name_json_capacity] = {};
    const char* name_encoding_error = nullptr;
    DWORD name_win32_error = ERROR_SUCCESS;
    const char* default_body_present = "null";
    bool count_valid = false;
    int feature_count = 0;
    QueryStatus current, type_query, name_query, body, features, size, free_array;
};

const char* nullable_status(const QueryStatus& status, char (&buffer)[32]) noexcept
{
    if (!status.called) {
        return "null";
    }
    const int count = std::snprintf(buffer, sizeof(buffer), "%d", static_cast<int>(status.code));
    return count > 0 && static_cast<size_t>(count) < sizeof(buffer) ? buffer : "null";
}

const char* model_type_name(ProMdlType type) noexcept
{
    switch (type) {
    case PRO_MDL_PART: return "PRO_MDL_PART";
    case PRO_MDL_ASSEMBLY: return "PRO_MDL_ASSEMBLY";
    case PRO_MDL_DRAWING: return "PRO_MDL_DRAWING";
    default: return "OTHER";
    }
}

void log_model_info_return(const ModelInfoResult& result) noexcept
{
    char current[32] = {}, type[32] = {}, name[32] = {}, body[32] = {};
    char features[32] = {}, size[32] = {}, freed[32] = {};
    char type_code[32] = {}, type_json[64] = {}, count_json[32] = {};
    if (result.type_valid) {
        std::snprintf(type_code, sizeof(type_code), "%d", static_cast<int>(result.type));
        std::snprintf(type_json, sizeof(type_json), "\"%s\"", model_type_name(result.type));
    }
    if (result.count_valid) {
        std::snprintf(count_json, sizeof(count_json), "%d", result.feature_count);
    }
    char facts[model_facts_capacity] = {};
    const int written = std::snprintf(facts, sizeof(facts),
        "{\"command\":\"p1_readonly_model_info\",\"result\":\"%s\","
        "\"no_active_model\":%s,\"model_type\":%s,\"model_type_code\":%s,"
        "\"model_name\":%s,\"default_body_present\":%s,\"feature_count\":%s,"
        "\"current_model_pro_error\":%s,\"type_pro_error\":%s,\"name_pro_error\":%s,"
        "\"default_body_pro_error\":%s,\"features_get_pro_error\":%s,"
        "\"array_size_pro_error\":%s,\"array_free_pro_error\":%s,"
        "\"name_encoding_error\":%s,\"name_win32_error\":%lu}",
        result.result, result.no_active_model,
        result.type_valid ? type_json : "null", result.type_valid ? type_code : "null",
        result.name_valid ? result.name_json : "null", result.default_body_present,
        result.count_valid ? count_json : "null",
        nullable_status(result.current, current), nullable_status(result.type_query, type),
        nullable_status(result.name_query, name), nullable_status(result.body, body),
        nullable_status(result.features, features), nullable_status(result.size, size),
        nullable_status(result.free_array, freed),
        result.name_encoding_error ? result.name_encoding_error : "null",
        static_cast<unsigned long>(result.name_win32_error));
    trace_event("MODEL_INFO_RETURN", written > 0 && static_cast<size_t>(written) < sizeof(facts)
        ? facts : "{\"command\":\"p1_readonly_model_info\",\"result\":\"format_failed\"}");
}

void read_part_name(ProMdl model, ModelInfoResult& result)
{
    ProMdlName name = {};
    result.name_query.called = true;
    result.name_query.code = ProMdlMdlnameGet(model, name);
    if (result.name_query.code != PRO_TK_NO_ERROR) {
        return;
    }
    size_t length = 0;
    while (length < model_name_units && name[length] != L'\0') {
        ++length;
    }
    if (length == model_name_units) {
        result.name_encoding_error = "\"unterminated_model_name\"";
        return;
    }
    char utf8[model_name_utf8_capacity] = {};
    if (length > 0) {
        const int count = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS,
            name, static_cast<int>(length), utf8, static_cast<int>(sizeof(utf8) - 1),
            nullptr, nullptr);
        if (count <= 0) {
            result.name_win32_error = GetLastError();
            result.name_encoding_error = "\"utf16_to_utf8_failed\"";
            return;
        }
        utf8[count] = '\0';
    }
    result.name_valid = quote_json_utf8(utf8, result.name_json, sizeof(result.name_json));
    if (!result.name_valid) {
        result.name_encoding_error = "\"json_name_capacity\"";
    }
}

int readonly_model_info_action(uiCmdCmdId command, uiCmdValue* value, void* push_data)
{
    PreserveLastError preserve;
    (void)command; (void)value; (void)push_data;
    trace_event("MODEL_INFO_ENTER", "{\"command\":\"p1_readonly_model_info\"}");
    ModelInfoResult result;
    ProMdl model = nullptr;
    result.current.called = true;
    result.current.code = ProMdlCurrentGet(&model);
    if (result.current.code == PRO_TK_BAD_CONTEXT) {
        result.result = "no_active_model";
        result.no_active_model = "true";
        log_model_info_return(result);
        return 0;
    }
    if (result.current.code != PRO_TK_NO_ERROR || model == nullptr) {
        result.result = "current_model_query_failed";
        log_model_info_return(result);
        return 0;
    }
    result.no_active_model = "false";
    result.type_query.called = true;
    result.type_query.code = ProMdlTypeGet(model, &result.type);
    if (result.type_query.code != PRO_TK_NO_ERROR) {
        result.result = "model_type_failed";
        log_model_info_return(result);
        return 0;
    }
    result.type_valid = true;
    if (result.type != PRO_MDL_PART) {
        result.result = "non_part_model";
        log_model_info_return(result);
        return 0;
    }
    read_part_name(model, result);
    // Only PRO_MDL_PART is converted to ProSolid. Never inspect feature content.
    ProSolid solid = reinterpret_cast<ProSolid>(model);
    ProSolidBody body = {};
    result.body.called = true;
    result.body.code = ProSolidDefaultBodyGet(solid, &body);
    if (result.body.code == PRO_TK_GENERAL_ERROR) {
        result.result = result.name_valid ? "no_default_body" : "partial_failure";
        result.default_body_present = "false";
        log_model_info_return(result);
        return 0;
    }
    if (result.body.code != PRO_TK_NO_ERROR) {
        result.result = "default_body_query_failed";
        log_model_info_return(result);
        return 0;
    }
    result.default_body_present = "true";
    ProFeature* features = nullptr;
    result.features.called = true;
    result.features.code = ProSolidBodyFeaturesGet(&body, &features);
    ProArray owned_array = features;
    if (result.features.code == PRO_TK_NO_ERROR && owned_array != nullptr) {
        result.size.called = true;
        result.size.code = ProArraySizeGet(owned_array, &result.feature_count);
        result.count_valid = result.size.code == PRO_TK_NO_ERROR && result.feature_count >= 0;
    }
    // Release every non-null returned array, including a failure-path allocation.
    if (owned_array != nullptr) {
        result.free_array.called = true;
        result.free_array.code = ProArrayFree(&owned_array);
    }
    const bool cleanup_ok = !result.free_array.called || result.free_array.code == PRO_TK_NO_ERROR;
    result.result = result.name_valid && result.count_valid && cleanup_ok ? "ok" : "partial_failure";
    log_model_info_return(result);
    return 0;
}

void trace_model_info_command_result(const char* event, ProError status) noexcept
{
    char facts[160] = {};
    const int count = std::snprintf(facts, sizeof(facts),
        "{\"command\":\"p1_readonly_model_info\",\"pro_error\":%d}",
        static_cast<int>(status));
    trace_event(event, count > 0 && static_cast<size_t>(count) < sizeof(facts) ? facts : "null");
}

void register_readonly_model_info()
{
    static char name[] = "p1_readonly_model_info";
    static char label[] = "P1_READONLY_MODEL_INFO_LABEL";
    static char help[] = "P1_READONLY_MODEL_INFO_HELP";
    static char description[] = "P1_READONLY_MODEL_INFO_DESC";
    static wchar_t message_file[] = L"p1_readonly_model_info.txt";
    uiCmdCmdId command_id = {};
    uiCmdCmdActFn action = &readonly_model_info_action;
    uiCmdAccessFn access = &model_access_observed;
    trace_event("MODEL_INFO_REGISTER_BEGIN", "{\"command\":\"p1_readonly_model_info\"}");
    const ProError add_status = ProCmdActionAdd(name, action, uiProeImmediate,
        access, PRO_B_TRUE, PRO_B_TRUE, &command_id);
    trace_model_info_command_result("MODEL_INFO_ACTION_ADD_RETURN", add_status);
    if (add_status != PRO_TK_NO_ERROR) {
        return;
    }
    trace_registration_identity("p1_readonly_model_info", command_id, name,
        label, help, description, message_file);
    const ProError designate_status = ProCmdDesignate(command_id, label, help,
        description, message_file);
    trace_model_info_command_result("MODEL_INFO_DESIGNATE_RETURN", designate_status);
    trace_command_lookup("p1_readonly_model_info", name, command_id, designate_status);
}

struct BodyFaceCountResult {
    const char* result = "query_failed";
    const char* no_active_model = "null";
    bool type_valid = false;
    ProMdlType type = {};
    const char* default_body_present = "null";
    bool count_valid = false;
    int face_count = 0;
    bool partial_count_valid = false;
    int partial_count = 0;
    QueryStatus current, type_query, body, visit;
};

void log_body_face_count_return(const BodyFaceCountResult& result) noexcept
{
    char current[32] = {}, type[32] = {}, body[32] = {}, visit[32] = {};
    char type_code[32] = {}, type_json[64] = {}, count_json[32] = {}, partial_json[32] = {};
    if (result.type_valid) {
        std::snprintf(type_code, sizeof(type_code), "%d", static_cast<int>(result.type));
        std::snprintf(type_json, sizeof(type_json), "\"%s\"", model_type_name(result.type));
    }
    if (result.count_valid) {
        std::snprintf(count_json, sizeof(count_json), "%d", result.face_count);
    }
    if (result.partial_count_valid) {
        std::snprintf(partial_json, sizeof(partial_json), "%d", result.partial_count);
    }
    char facts[1024] = {};
    const int written = std::snprintf(facts, sizeof(facts),
        "{\"command\":\"p1_readonly_body_face_count\",\"result\":\"%s\","
        "\"no_active_model\":%s,\"model_type\":%s,\"model_type_code\":%s,"
        "\"default_body_present\":%s,\"face_count\":%s,\"visited_count_before_error\":%s,"
        "\"current_model_pro_error\":%s,\"type_pro_error\":%s,"
        "\"default_body_pro_error\":%s,\"surface_visit_pro_error\":%s}",
        result.result, result.no_active_model,
        result.type_valid ? type_json : "null", result.type_valid ? type_code : "null",
        result.default_body_present, result.count_valid ? count_json : "null",
        result.partial_count_valid ? partial_json : "null",
        nullable_status(result.current, current), nullable_status(result.type_query, type),
        nullable_status(result.body, body), nullable_status(result.visit, visit));
    trace_event("BODY_FACE_COUNT_RETURN", written > 0 && static_cast<size_t>(written) < sizeof(facts)
        ? facts : "{\"command\":\"p1_readonly_body_face_count\",\"result\":\"format_failed\"}");
}

ProError count_default_body_surface(ProSurface surface, ProError status, ProAppData app_data)
{
    (void)surface;
    (void)status;
    // app_data is our local, initialized int; no surface is inspected or retained.
    int* surface_count = static_cast<int*>(app_data);
    ++(*surface_count);
    return PRO_TK_NO_ERROR;
}

int readonly_body_face_count_action(uiCmdCmdId command, uiCmdValue* value, void* push_data)
{
    PreserveLastError preserve;
    (void)command; (void)value; (void)push_data;
    trace_event("BODY_FACE_COUNT_ENTER", "{\"command\":\"p1_readonly_body_face_count\"}");
    BodyFaceCountResult result;
    ProMdl model = nullptr;
    result.current.called = true;
    result.current.code = ProMdlCurrentGet(&model);
    if (result.current.code == PRO_TK_BAD_CONTEXT) {
        result.result = "no_active_model";
        result.no_active_model = "true";
        log_body_face_count_return(result);
        return 0;
    }
    if (result.current.code != PRO_TK_NO_ERROR || model == nullptr) {
        result.result = "current_model_query_failed";
        log_body_face_count_return(result);
        return 0;
    }
    result.no_active_model = "false";
    result.type_query.called = true;
    result.type_query.code = ProMdlTypeGet(model, &result.type);
    if (result.type_query.code != PRO_TK_NO_ERROR) {
        result.result = "model_type_failed";
        log_body_face_count_return(result);
        return 0;
    }
    result.type_valid = true;
    if (result.type != PRO_MDL_PART) {
        result.result = "non_part_model";
        log_body_face_count_return(result);
        return 0;
    }
    ProSolid solid = reinterpret_cast<ProSolid>(model);
    ProSolidBody body = {};
    result.body.called = true;
    result.body.code = ProSolidDefaultBodyGet(solid, &body);
    if (result.body.code == PRO_TK_GENERAL_ERROR) {
        result.result = "no_default_body";
        result.default_body_present = "false";
        log_body_face_count_return(result);
        return 0;
    }
    if (result.body.code != PRO_TK_NO_ERROR) {
        result.result = "default_body_query_failed";
        log_body_face_count_return(result);
        return 0;
    }
    result.default_body_present = "true";
    int surface_count = 0;
    ProSurfaceVisitAction visitor = &count_default_body_surface;
    result.visit.called = true;
    result.visit.code = ProSolidBodySurfaceVisit(&body, visitor, &surface_count);
    if (result.visit.code == PRO_TK_NO_ERROR) {
        result.result = "ok";
        result.count_valid = true;
        result.face_count = surface_count;
    } else if (result.visit.code == PRO_TK_E_NOT_FOUND && surface_count == 0) {
        result.result = "no_faces";
        result.count_valid = true;
        result.face_count = 0;
    } else {
        result.result = "surface_visit_failed";
        result.partial_count_valid = true;
        result.partial_count = surface_count;
    }
    log_body_face_count_return(result);
    return 0;
}

void trace_body_face_count_command_result(const char* event, ProError status) noexcept
{
    char facts[160] = {};
    const int count = std::snprintf(facts, sizeof(facts),
        "{\"command\":\"p1_readonly_body_face_count\",\"pro_error\":%d}",
        static_cast<int>(status));
    trace_event(event, count > 0 && static_cast<size_t>(count) < sizeof(facts) ? facts : "null");
}

void register_readonly_body_face_count()
{
    static char name[] = "p1_readonly_fc";
    static char label[] = "P1_BODY_FACE_COUNT_LABEL";
    static char help[] = "P1_BODY_FACE_COUNT_HELP";
    static char description[] = "P1_BODY_FACE_COUNT_DESC";
    static wchar_t message_file[] = L"p1_fc.txt";
    uiCmdCmdId command_id = {};
    uiCmdCmdActFn action = &readonly_body_face_count_action;
    uiCmdAccessFn access = &body_access_observed;
    trace_event("BODY_FACE_COUNT_REGISTER_BEGIN", "{\"command\":\"p1_readonly_body_face_count\"}");
    const ProError add_status = ProCmdActionAdd(name, action, uiProeImmediate,
        access, PRO_B_TRUE, PRO_B_TRUE, &command_id);
    trace_body_face_count_command_result("BODY_FACE_COUNT_ACTION_ADD_RETURN", add_status);
    if (add_status != PRO_TK_NO_ERROR) {
        return;
    }
    trace_registration_identity("p1_readonly_body_face_count", command_id, name,
        label, help, description, message_file);
    const ProError designate_status = ProCmdDesignate(command_id, label, help,
        description, message_file);
    trace_body_face_count_command_result("BODY_FACE_COUNT_DESIGNATE_RETURN", designate_status);
    trace_command_lookup("p1_readonly_body_face_count", name, command_id, designate_status);
}

// MESSAGE_JSON_HELPER_BEGIN
// Encode at most 80 UTF-16 code units as ASCII JSON; reject invalid pairs.
// This helper has no SDK or Windows calls and never scans beyond units.
bool quote_message_utf16(const wchar_t* input, size_t units,
    char* output, size_t capacity) noexcept
{
    if (!output || capacity == 0) return false;
    output[0] = '\0';
    if (!input || units > 80 || capacity < units * 6 + 3) return false;
    for (size_t i = 0; i < units; ++i) {
        const unsigned long unit = static_cast<unsigned long>(input[i]);
        if (unit > 0xffff) return false;
        if (unit >= 0xd800 && unit <= 0xdbff) {
            if (i + 1 >= units) return false;
            const unsigned long low = static_cast<unsigned long>(input[++i]);
            if (low < 0xdc00 || low > 0xdfff) return false;
        } else if (unit >= 0xdc00 && unit <= 0xdfff) {
            return false;
        }
    }
    const char hex[] = "0123456789abcdef";
    size_t used = 0;
    output[used++] = '"';
    for (size_t i = 0; i < units; ++i) {
        const unsigned long unit = static_cast<unsigned long>(input[i]);
        output[used++] = '\\'; output[used++] = 'u';
        output[used++] = hex[(unit >> 12) & 0xf];
        output[used++] = hex[(unit >> 8) & 0xf];
        output[used++] = hex[(unit >> 4) & 0xf];
        output[used++] = hex[unit & 0xf];
    }
    output[used++] = '"'; output[used] = '\0';
    return true;
}
// MESSAGE_JSON_HELPER_END

static_assert(PRO_LINE_SIZE == 81, "Verify supplied ProLine capacity contract");
static_assert(sizeof(wchar_t) == 2, "The probe logs Windows UTF-16 code units");

void probe_one_message(const char* role, ProFileName file_name,
    ProCharLine message_key) noexcept
{
    PreserveLastError preserve;
    char begin[192] = {};
    std::snprintf(begin, sizeof(begin),
        "{\"revision\":\"UI_R6_MESSAGE_PROBE\",\"role\":\"%s\",\"key\":\"%s\"}", role, message_key);
    trace_event("MESSAGE_BUFFER_PROBE_BEGIN", begin);
    ProLine translated = {};
    // These fixed resource messages have no conversion placeholders.
    // The supplied official example uses no trailing variadic sentinel.
    const ProError status = ProMessageToBuffer(translated, file_name, message_key);
    const bool readable = status == PRO_TK_NO_ERROR || status == PRO_TK_MSG_TOO_LONG;
    size_t units = 0;
    bool terminated = false;
    bool encoded = false;
    char text_json[80 * 6 + 3] = {};
    if (readable) {
        while (units < PRO_LINE_SIZE && translated[units] != 0) ++units;
        terminated = units < PRO_LINE_SIZE;
        if (terminated) encoded = quote_message_utf16(translated, units,
            text_json, sizeof(text_json));
    }
    char units_json[32] = {};
    if (readable) std::snprintf(units_json, sizeof(units_json), "%u", static_cast<unsigned>(units));
    // Fixed role/key literals are ASCII without JSON metacharacters.
    char facts[1024] = {};
    const int count = std::snprintf(facts, sizeof(facts),
        "{\"revision\":\"UI_R6_MESSAGE_PROBE\",\"phase\":\"after_all_designations\","
        "\"role\":\"%s\",\"key\":\"%s\",\"pro_error\":%d,"
        "\"buffer_observed\":%s,\"terminated\":%s,\"wide_units\":%s,"
        "\"text_json_valid\":%s,\"sdk_reported_truncation\":%s,\"text\":%s,"
        "\"resolved_path_exposed\":false}",
        role, message_key, static_cast<int>(status), readable ? "true" : "false",
        readable ? (terminated ? "true" : "false") : "null",
        readable ? units_json : "null", encoded ? "true" : "false",
        status == PRO_TK_MSG_TOO_LONG ? "true" : "false", encoded ? text_json : "null");
    if (count <= 0 || static_cast<size_t>(count) >= sizeof(facts)) {
        trace_failure("message_probe_format", ERROR_INSUFFICIENT_BUFFER);
        return;
    }
    trace_event("MESSAGE_BUFFER_PROBE_RETURN", facts);
}

void probe_designation_messages() noexcept
{
    ProFileName body_file = L"p1_fc.txt";
    ProFileName control_file = L"p1_readonly_model_info.txt";
    ProCharLine label_key = "P1_BODY_FACE_COUNT_LABEL";
    ProCharLine help_key = "P1_BODY_FACE_COUNT_HELP";
    ProCharLine description_key = "P1_BODY_FACE_COUNT_DESC";
    ProCharLine control_key = "P1_READONLY_MODEL_INFO_LABEL";
    probe_one_message("body_label", body_file, label_key);
    probe_one_message("body_help", body_file, help_key);
    probe_one_message("body_description", body_file, description_key);
    probe_one_message("control_model_info_label", control_file, control_key);
    ProFileName ping_file = L"p1_readonly_ping.txt";
    ProCharLine control_description_key = "P1_READONLY_MODEL_INFO_DESC";
    ProCharLine ping_description_key = "P1_READONLY_PING_DESCRIPTION";
    probe_one_message("control_model_info_description", control_file, control_description_key);
    probe_one_message("control_ping_description", ping_file, ping_description_key);
}

} // namespace

extern "C" __declspec(dllexport)
int user_initialize(int argc, char *argv[], char *version, char *build,
                    wchar_t errbuf[80])
{
    PreserveLastError preserve;
    // Inspect only scalar values and pointer presence. Never dereference host buffers.
    (void)argv;
    char facts[192] = {};
    const int count = std::snprintf(facts, sizeof(facts),
        "{\"argc\":%d,\"version_ptr_present\":%s,\"build_ptr_present\":%s,"
        "\"errbuf_ptr_present\":%s}", argc, version ? "true" : "false",
        build ? "true" : "false", errbuf ? "true" : "false");
    trace_event("INIT_ENTER", count > 0 && static_cast<size_t>(count) < sizeof(facts)
        ? facts : "null");
    register_readonly_body_face_count();
    register_readonly_model_info();
    register_readonly_ping();
    probe_designation_messages();
    flush_access_observations("initialization_end");
    trace_event("INIT_BEFORE_RETURN", "{\"return_code\":0}");
    // Trace success/failure never changes this return value.
    return PRO_TK_NO_ERROR;
}

extern "C" __declspec(dllexport)
void user_terminate(void)
{
    PreserveLastError preserve;
    trace_event("TERM_ENTER", "null");
    flush_access_observations("termination");
    trace_event("TERM_BEFORE_RETURN", "null");
}
