#include <cstddef>
#include <cstdio>
#include <cassert>
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

int main() {
    char out[483] = {};
    wchar_t text[] = {0x4e2d, 0x6587, 0x22, 0x5c, 0x0a, 0xd83d, 0xde00};
    assert(quote_message_utf16(text, 7, out, sizeof(out))); std::puts(out);
    wchar_t full[80]; for (size_t i=0;i<80;++i) full[i]=0x41;
    assert(quote_message_utf16(full,80,out,sizeof(out))); std::puts(out);
    assert(!quote_message_utf16(full,80,out,482));
    wchar_t high[] = {0xd800}; wchar_t low[] = {0xdc00};
    assert(!quote_message_utf16(high,1,out,sizeof(out)));
    assert(!quote_message_utf16(low,1,out,sizeof(out)));
    assert(!quote_message_utf16(full,81,out,sizeof(out)));
    assert(quote_message_utf16(full,0,out,sizeof(out))); std::puts(out);
}
