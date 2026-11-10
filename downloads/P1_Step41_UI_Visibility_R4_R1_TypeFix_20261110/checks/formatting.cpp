#include <cstdio>
#include <cstdint>
#include <cinttypes>
#include <limits>
#include <sstream>
#include <iostream>
int main() {
    const char* logical_command = "p1_readonly_body_face_count";
    const char* sdk_action_name = "p1_readonly_fc";
    const char* label_key = "P1_BODY_FACE_COUNT_LABEL";
    const char* help_key = "P1_BODY_FACE_COUNT_HELP";
    const char* description_key = "P1_BODY_FACE_COUNT_DESC";
    const char* ascii_file = "p1_fc.txt";
    int objects[2] = {};
    void* ids[] = {nullptr, &objects[0], &objects[0], &objects[1]};
    for (void* command_id : ids) {
        char facts[768] = {};
    const int count = std::snprintf(facts, sizeof(facts),
        "{\"revision\":\"UI_R4_R1_DIAG\",\"command\":\"%s\","
        "\"command_id\":\"0x%" PRIxPTR "\",\"sdk_action_name\":\"%s\","
        "\"label_key\":\"%s\",\"help_key\":\"%s\","
        "\"description_key\":\"%s\",\"message_file\":\"%s\"}",
        logical_command, reinterpret_cast<std::uintptr_t>(command_id), sdk_action_name,
        label_key, help_key, description_key, ascii_file);
        if (count <= 0 || static_cast<size_t>(count) >= sizeof(facts)) return 1;
        std::ostringstream expected;
        expected << "0x" << std::hex << reinterpret_cast<std::uintptr_t>(command_id);
        std::cout << facts << "\n" << expected.str() << "\n";
    }
    // Exercise full-width integer formatting separately; no fabricated pointer.
    char largest[2 * sizeof(std::uintptr_t) + 3] = {};
    std::snprintf(largest, sizeof(largest), "0x%" PRIxPTR,
        std::numeric_limits<std::uintptr_t>::max());
    std::cout << largest << "\n";
}
