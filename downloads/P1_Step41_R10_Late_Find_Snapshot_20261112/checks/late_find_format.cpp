#include <cstdio>
#include <cstddef>
#include <cstdint>
#include <cinttypes>
#include <cassert>
// LATE_FIND_FORMAT_HELPER_BEGIN
bool quote_late_pointer(const void* pointer, char* output, size_t capacity) noexcept
{
    const int n = pointer == nullptr
        ? std::snprintf(output, capacity, "null")
        : std::snprintf(output, capacity, "\"0x%" PRIxPTR "\"",
            reinterpret_cast<std::uintptr_t>(pointer));
    return n > 0 && static_cast<size_t>(n) < capacity;
}

bool format_late_find(char* facts, size_t capacity, const char* command,
    const char* sdk_name, unsigned command_ordinal, unsigned long long tick,
    bool initialized, bool valid_pair, int baseline_status,
    const void* baseline_add, const void* baseline_find,
    int late_status, bool late_success, const void* late_found) noexcept
{
    char add_json[2 * sizeof(std::uintptr_t) + 5] = {};
    char initial_json[2 * sizeof(std::uintptr_t) + 5] = {};
    char late_json[2 * sizeof(std::uintptr_t) + 5] = {};
    if (!quote_late_pointer(initialized && valid_pair ? baseline_add : nullptr, add_json, sizeof(add_json)) ||
        !quote_late_pointer(initialized && valid_pair ? baseline_find : nullptr, initial_json, sizeof(initial_json)) ||
        !quote_late_pointer(late_success ? late_found : nullptr, late_json, sizeof(late_json))) return false;
    char baseline_code[32] = {};
    if (initialized) std::snprintf(baseline_code, sizeof(baseline_code), "%d", baseline_status);
    const bool comparable = initialized && valid_pair && late_success;
    const int n = std::snprintf(facts, capacity,
        "{\"revision\":\"UI_R10_LATE_FIND\",\"phase\":\"on_demand_ping_action\","
        "\"probe_ordinal\":1,\"command_ordinal\":%u,\"probe_tick\":%llu,"
        "\"command\":\"%s\",\"sdk_action_name\":\"%s\",\"find_pro_error\":%d,"
        "\"baseline_initialized\":%s,\"baseline_valid_pair\":%s,\"baseline_find_pro_error\":%s,"
        "\"baseline_action_add_id\":%s,\"baseline_find_id\":%s,\"late_found_id\":%s,"
        "\"late_found_id_null\":%s,\"id_matches_baseline_action_add\":%s,"
        "\"id_matches_baseline_find\":%s,\"UI_membership_read_back\":false}",
        command_ordinal, tick, command, sdk_name, late_status,
        initialized ? "true" : "false", initialized && valid_pair ? "true" : "false",
        initialized ? baseline_code : "null", add_json, initial_json, late_json,
        late_success ? (late_found == nullptr ? "true" : "false") : "null",
        comparable ? (late_found != nullptr && late_found == baseline_add ? "true" : "false") : "null",
        comparable ? (late_found != nullptr && late_found == baseline_find ? "true" : "false") : "null");
    return n > 0 && static_cast<size_t>(n) < capacity;
}
// LATE_FIND_FORMAT_HELPER_END

int main() {
    int objects[3] = {}; char facts[1536] = {};
    assert(format_late_find(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",1,9007199254740993ULL,true,true,0,&objects[0],&objects[0],0,true,&objects[0])); std::puts(facts);
    assert(format_late_find(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",1,123,true,true,0,&objects[0],&objects[1],0,true,&objects[1])); std::puts(facts);
    assert(format_late_find(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",1,123,true,true,0,&objects[0],&objects[0],0,true,nullptr)); std::puts(facts);
    assert(format_late_find(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",1,123,true,true,0,&objects[0],&objects[0],-1,false,nullptr)); std::puts(facts);
    assert(format_late_find(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",1,123,true,false,-1,nullptr,nullptr,0,true,&objects[0])); std::puts(facts);
    assert(format_late_find(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",1,123,false,false,0,nullptr,nullptr,0,true,&objects[0])); std::puts(facts);
    assert(!format_late_find(facts,8,"p1_readonly_body_face_count","p1_readonly_fc",1,123,false,false,0,nullptr,nullptr,0,true,nullptr));
}
