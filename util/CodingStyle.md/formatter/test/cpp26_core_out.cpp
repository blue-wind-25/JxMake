template<typename... T>
using Nth = T...[N];

template<typename... T>
using Selected = T...[ computeIndex() ];

template<typename... T>
using Rebased = T...[ offsets[i] ];

void oldApi() = delete("use newApi() instead");
void reallyOldApi() = delete;

auto [_, count]    = getResult();
auto [_, _, total] = getTriple();
if( auto _ = acquireLock(); true ) doWork();

int divide(int a, int b)
    pre(b != 0)
    post(r: r * b == a)
{
    return a / b;
}

int clampSimple(int x) pre(x >= 0) { return x; }

int clampFull(int x, int lo, int hi)
    pre(lo <= hi)
    pre(x >= lo)
    post(r: r <= hi)
{
    if(x < lo) return lo;
    if(x > hi) return hi;

    return x;
}

void process(int x)
{
    contract_assert(x >= 0);
    contract_assert(x < 1000);
}
