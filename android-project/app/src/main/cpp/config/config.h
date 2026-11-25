/* config.h for the Android build of AdvanceMAME.
 * Hand-written replacement for the autoconf-generated config.h.
 * ABI-dependent sizes are resolved with __LP64__.
 */
#ifndef ANDROID_CONFIG_H
#define ANDROID_CONFIG_H

#define PACKAGE_NAME "AdvanceMAME"
#define PACKAGE_STRING "AdvanceMAME 5.0"
#define PACKAGE_TARNAME "advancemame"
#define PACKAGE_VERSION "5.0"
#define PACKAGE_URL "http://www.advancemame.it/"
#define PACKAGE_BUGREPORT "advancemame-users@lists.sourceforge.net"

#define STDC_HEADERS 1
#define HAVE_UNISTD_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRING_H 1
#define HAVE_STRINGS_H 1
#define HAVE_STDINT_H 1
#define HAVE_INTTYPES_H 1
#define HAVE_TIME_H 1
#define HAVE_DIRENT_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TIME_H 1
#define HAVE_SYS_PARAM_H 1
#define HAVE_SYS_IOCTL_H 1
#define HAVE_SYS_SOCKET_H 1
#define HAVE_SYS_UTSNAME_H 1
#define HAVE_SYS_MMAN_H 1
#define HAVE_SYS_SELECT_H 1
#define HAVE_SYS_WAIT_H 1
#define HAVE_NETDB_H 1
#define HAVE_NETINET_IN_H 1
#define HAVE_TERMIOS_H 1
#define HAVE_SCHED_H 1
#define HAVE_UCONTEXT_H 1

#define HAVE_ALLOCA 1
#define HAVE_ALLOCA_H 1
#define HAVE_STRCASECMP 1
#define HAVE_STRERROR 1
#define HAVE_SYSCONF 1
#define HAVE_VPRINTF 1
#define HAVE_UNAME 1
#define HAVE_GETPAGESIZE 1
#define HAVE_MMAP 1
#define HAVE_MPROTECT 1
#define HAVE_FSEEKO 1
#define HAVE_FTELLO 1
#define HAVE_UTIMES 1
#define HAVE_SCHED_YIELD 1
#define HAVE_SCHED_GETSCHEDULER 1
#define HAVE_SCHED_GET_PRIORITY_MAX 1
#define HAVE_SCHED_SETSCHEDULER 0
#define HAVE_LIBPTHREAD 1
#define HAVE_FDOPEN 1
#define HAVE_BACKTRACE 0
#define HAVE_BACKTRACE_SYMBOLS 0

#ifdef __LP64__
#define SIZEOF_CHAR 1
#define SIZEOF_SHORT 2
#define SIZEOF_INT 4
#define SIZEOF_LONG 8
#define SIZEOF_LONG_LONG 8
#define SIZEOF_VOIDP 8
#else
#define SIZEOF_CHAR 1
#define SIZEOF_SHORT 2
#define SIZEOF_INT 4
#define SIZEOF_LONG 4
#define SIZEOF_LONG_LONG 8
#define SIZEOF_VOIDP 4
#endif

#endif
