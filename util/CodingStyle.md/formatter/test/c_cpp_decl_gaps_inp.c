// Regression coverage for three DeclarationAlignmentRule fixes recorded under
// STATE.md's "Known Gaps -- Fixed": the `* const` column gap, `typedef`
// alignment inside a plain-variable group, and direct function-pointer
// declarations (including multi-star `(**cb)`) joining a surrounding group.

void gapExample(void)
{
    char** a;
    double** b;
    char* const c;
}

void typedefExample(void)
{
    typedef unsigned char byte;
    int count;
    double ratio;
}

void functionPointerExample(void)
{
    int count = 0;
    void (*cb)(int) = NULL;
    float ratio = 1.0f;
    void (**cbcb)(int) = NULL;
}
