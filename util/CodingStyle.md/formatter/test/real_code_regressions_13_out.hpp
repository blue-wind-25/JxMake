/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

# if !((__cplusplus >= 201103L) && defined(_GLIBCXX_USE_C99) \
           && !defined(_GLIBCXX_HAVE_BROKEN_VSWPRINTF))
#define CATCH_INTERNAL_CONFIG_NO_CPP11_TO_STRING
# endif

namespace Catch {

    inline std::size_t registerTestMethods()
    {
        int noClasses = objc_getClassList(nullptr, 0);
        for(int c = 0; c < noClasses; ++c) {
            Class cls = classes[c];
            {
                u_int   count;
                Method* methods = class_copyMethodList(cls,& count);
                for(u_int m = 0; m < count ; ++m) {
                    SEL         selector   = method_getName( methods[m] );
                    std::string methodName = sel_getName(selector);
                }
            }
        } // for c

        return noTestMethods;
    }

} // namespace Catch
