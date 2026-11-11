#include <cstdio>
#include <cstddef>
#include <cstdint>
#include <cinttypes>
#include <cassert>
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
        "{\"revision\":\"UI_R9_COMMAND_LOOKUP\",\"phase\":\"after_designate_return\","
        "\"command\":\"%s\",\"sdk_action_name\":\"%s\",\"pro_error\":%d,"
        "\"designate_pro_error\":%d,\"action_add_id\":\"0x%" PRIxPTR "\","
        "\"lookup_id\":%s,\"lookup_id_is_null\":%s,\"ids_equal\":%s,"
        "\"UI_membership_read_back\":false}",
        logical_command, sdk_name, status, designate_status,
        reinterpret_cast<std::uintptr_t>(expected), success ? found_json : "null",
        success ? (found == nullptr ? "true" : "false") : "null",
        success ? (expected == found ? "true" : "false") : "null");
    return n > 0 && static_cast<size_t>(n) < capacity;
}
// LOOKUP_FORMAT_HELPER_END

int main() {
 int objects[2]={}; char facts[512]={};
 assert(format_lookup_facts(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",&objects[0],&objects[0],0,0,true)); std::puts(facts);
 assert(format_lookup_facts(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",&objects[0],&objects[1],0,0,true)); std::puts(facts);
 assert(format_lookup_facts(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",&objects[0],nullptr,0,0,true)); std::puts(facts);
 assert(format_lookup_facts(facts,sizeof(facts),"p1_readonly_body_face_count","p1_readonly_fc",&objects[0],nullptr,-1,0,false)); std::puts(facts);
 assert(!format_lookup_facts(facts,8,"p1_readonly_body_face_count","p1_readonly_fc",nullptr,nullptr,0,0,true));
}
