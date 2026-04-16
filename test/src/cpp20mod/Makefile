# Tested using
#     Cygwin64 g++ (GCC) 11.3.0
#     Linux64  g++ (GCC) 12.2.0

ifneq ($(shell uname -s | grep -i cygwin),)
    GPP = g++
else
    F_NVIA_ERR_FN = _err.txt
    FILTER_NVIA_N = 2>$(F_NVIA_ERR_FN); ERR=$$?; cat $(F_NVIA_ERR_FN) | grep -v 'no version information available'; rm -f $(F_NVIA_ERR_FN); exit $$ERR
    GPP           = LD_LIBRARY_PATH=/opt/isl-0.16.1/lib:/opt/gcc-12.2.0/lib64 /opt/gcc-12.2.0/bin/g++ -fdiagnostics-color=always
endif

OBJ_FILES = main.cpp.o              \
            userlib.cpp.o           \
            userlib_impl.cpp.o      \
            sys/syslib1.cpp.o       \
            sys/syslib1_impl1.cpp.o \
            sys/syslib1_impl2.cpp.o \
            sys/syslib2.cpp.o       \
            sys/syslib2-one.cpp.o   \
            sys/syslib2-two.cpp.o   \
            sys/syslib2-three.cpp.o \
            sys/syslib3.cpp.o       \
            sys/syslib3-zero.cpp.o  \
            sys/syslib3-one.cpp.o   \
            sys/syslib3-two.cpp.o

default: main.exe
	@./main.exe

main.exe: ${OBJ_FILES}
	@echo $^
	@$(GPP) -std=c++20 -fmodules-ts $^ -o $@ $(FILTER_NVIA_N)

%.cpp.o: %.cpp
	@echo $^
	@$(GPP) -Isys -std=c++20 -fmodules-ts -c $< -o $@ $(FILTER_NVIA_N)

%.sys.unit:
	@echo $^
	@$(GPP) -std=c++20 -fmodules-ts -x c++-system-header -c $(@:%.sys.unit=%) $(FILTER_NVIA_N)
	@touch $@

%.h.usr.unit: %.h
	@echo $^
	@$(GPP) -std=c++20 -fmodules-ts -fmodule-header -c $(@:%.h.usr.unit=%.h) $(FILTER_NVIA_N)
	@touch $@

clean:
	@rm -rvf gcm.cache
	@rm -rvf pcm.cache
	@rm  -vf *.pcm
	@rm  -vf deplist.d
	@rm  -vf *.unit
	@rm  -vf a.exe
	@rm  -vf a.out
	@rm  -vf main.exe
	@rm  -vf main.elf
	@rm  -vf *.cpp.o
	@rm  -vf *.d
	@rm  -vf sys/*.cpp.o
	@rm  -vf sys/*.d
	@touch Makefile
	@touch *.cpp
	@touch sys/*.cpp


####################################################################################################


main.cpp.o: user_hu.h.usr.unit sys/syslib1.cpp.o userlib.cpp.o

sys/syslib1_impl1.cpp.o: sys/syslib1.cpp.o

sys/syslib1_impl2.cpp.o: sys/syslib1.cpp.o

sys/syslib2.cpp.o: sys/syslib2-one.cpp.o sys/syslib2-two.cpp.o sys/syslib2-three.cpp.o

sys/syslib3-zero.cpp.o: sys/syslib3-one.cpp.o sys/syslib3-two.cpp.o

sys/syslib3.cpp.o: sys/syslib3-zero.cpp.o

userlib.cpp.o: sys/syslib2.cpp.o sys/syslib3.cpp.o iostream.sys.unit

userlib_impl.cpp.o: userlib.cpp.o iostream.sys.unit
