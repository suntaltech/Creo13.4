// Step39: one manually invoked JSONL ping command; no model API calls.
// Entry ABI is unchanged; actual SDK documentation remains authoritative.
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <ProToolkit.h>
#include <ProToolkitDll.h>
#include <ProUICmd.h>
#include <ProMenuBar.h>
#include <windows.h>
#include <cstdio>

static_assert(PRO_TK_NO_ERROR == 0, "Verify actual SDK success-code definition");

namespace {
volatile LONG trace_sequence = 0;

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
    char line[768] = {};
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
    uiCmdAccessFn access = &readonly_ping_access;
    trace_event("CMD_REGISTER_BEGIN", "{\"command\":\"p1_readonly_ping\"}");
    const ProError add_status = ProCmdActionAdd(name, action, uiProeImmediate,
        access, PRO_B_TRUE, PRO_B_TRUE, &command_id);
    trace_command_result("CMD_ACTION_ADD_RETURN", add_status);
    if (add_status != PRO_TK_NO_ERROR) {
        // Do not reuse or overwrite a possibly stale, same-name command.
        return;
    }
    const ProError designate_status = ProCmdDesignate(command_id, label,
        help, description, message_file);
    trace_command_result("CMD_DESIGNATE_RETURN", designate_status);
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
    register_readonly_ping();
    trace_event("INIT_BEFORE_RETURN", "{\"return_code\":0}");
    // Trace success/failure never changes this return value.
    return PRO_TK_NO_ERROR;
}

extern "C" __declspec(dllexport)
void user_terminate(void)
{
    PreserveLastError preserve;
    trace_event("TERM_ENTER", "null");
    trace_event("TERM_BEFORE_RETURN", "null");
}
