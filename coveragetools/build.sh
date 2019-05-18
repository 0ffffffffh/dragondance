#!/bin/sh

#set your own pin root directory
PIN_ROOT=/home/kartal/pin

CL_DEFS="-Wno-unknown-pragmas -D__PIN__=1 -DPIN_CRT=1"
CL_FLAG="-fno-exceptions -funwind-tables -fno-rtti -fno-stack-protector -fpermissive -fPIC "
LD_FLAG=""

CL_LINKER_LIBS="-lpin -lxed -lc-dynamic -lm-dynamic -lstlport-dynamic -lpin3dwarf -lunwind-dynamic"


INC_BASE="-isystem $PIN_ROOT"


if [ "$1" = "x32" ]; then
	TARGET=ia32
	BIONIC_ARCH=x86
	ARCH_SUFFIX="32"
	CL_DEFS="$CL_DEFS -DTARGET_IA32 -D__i386__ -DHOST_IA32"

	if [ $(uname -m | grep "64") != "" ]; then
		echo "Host system is 64 bit but 32 bit compilation requested. So added -m32 flag to achieve the cross compiling"
		CL_FLAG="-m32 $CL_FLAG"
		LD_FLAG="-m32"
	fi
else
	TARGET=intel64
	ARCH_SUFFIX="64"
	BIONIC_ARCH=x86_64
	CL_DEFS="$CL_DEFS -DTARGET_IA32E -D__x86_64__ -DHOST_IA32E"
fi


PINCRT_ROOT="$PIN_ROOT/$TARGET/runtime/pincrt"

if [ "$(g++ -v 2>&1 | grep 'Apple')" != "" ]; then
    COMP_SPECIFIC="-Wl,-no_new_main"
else
    COMP_SPECIFIC="-Wl,--hash-style=sysv -Wl,-Bsymbolic"
fi

if [ "$2" = "-oldabi" ]; then
	#GXX's new c++ abi standard breaks intel pin sdk build.
	#so we need to set older version of the ABI.
	CL_DEFS="$CL_DEFS -D_GLIBCXX_USE_CXX11_ABI=0"
	CL_FLAG="$CL_FLAG -fabi-version=2"
	LD_FLAG="$LD_FLAG -fabi-version=2"
fi

if [ "$(uname -s)" = 'Darwin' ]; then
    BIN_SUFFIX="dylib"
    CL_DEFS="$CL_DEFS -DTARGET_MAC -D__DARWIN_ONLY_UNIX_CONFORMANCE=1 -D__DARWIN_UNIX03=0"
else
    CL_DEFS="$CL_DEFS -DTARGET_LINUX"
    BIN_SUFFIX="so"
    LD_FLAG="$LD_FLAG -Wl,$PINCRT_ROOT/crtendS.o"
    CL_LINKER_LIBS="$CL_LINKER_LIBS -ldl-dynamic"
fi


CL_LINKER_LIB_DIRS="-L$PIN_ROOT/$TARGET/runtime/pincrt -L$PIN_ROOT/extras/xed-$TARGET/lib -L$PIN_ROOT/$TARGET/lib -L$PIN_ROOT/$TARGET/lib-ext"

CL_LINKER="-nostdlib $CL_LINKER_LIB_DIRS $CL_LINKER_LIBS"


INCLUDES="$INC_BASE/source/include/pin/gen "
INCLUDES="$INCLUDES $INC_BASE/source/include/pin "
INCLUDES="$INCLUDES $INC_BASE/extras/stlport/include "
INCLUDES="$INCLUDES $INC_BASE/extras/libstdc++/include "
INCLUDES="$INCLUDES $INC_BASE/extras/crt/include "
INCLUDES="$INCLUDES $INC_BASE/extras/crt/include/arch-$BIONIC_ARCH "
INCLUDES="$INCLUDES $INC_BASE/extras/crt/include/kernel/uapi "
INCLUDES="$INCLUDES $INC_BASE/extras/crt/include/kernel/uapi/asm-x86 "
INCLUDES="$INCLUDES $INC_BASE/extras/components/include "
INCLUDES="$INCLUDES $INC_BASE/extras/xed-$TARGET/include/xed "

#compile source without linking
eval "g++ $CL_DEFS $CL_FLAG $INCLUDES -c -o ddph.o ddph.c"

#link main program object file with required intel pin shared libs and build .so
eval "g++ -shared $COMP_SPECIFIC -Wl,$PINCRT_ROOT/crtbeginS.o -o ddph$ARCH_SUFFIX.$BIN_SUFFIX ddph.o $CL_LINKER $LD_FLAG"

