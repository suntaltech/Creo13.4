// Candidate entry ABI: verify against the installed SDK and its official docs.
// No business API, file I/O, UI, model access, or DllMain is implemented here.
#include <ProToolkit.h>
#include <ProToolkitDll.h>

extern "C" __declspec(dllexport)
int user_initialize(int argc, char *argv[], char *version, char *build,
                    wchar_t errbuf[80])
{
    (void)argc;
    (void)argv;
    (void)version;
    (void)build;
    (void)errbuf;
    return PRO_TK_NO_ERROR;
}

extern "C" __declspec(dllexport)
void user_terminate(void)
{
}
