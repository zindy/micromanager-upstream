# This file is a modified version of ax_jni_include_dir.m4 from the Autoconf
# archive.
# The changes
# - prevent failure when JAVAC is set to an absolute path,
# - when JAVA_HOME is set, disable OS X specific handling of javac-relative
#   paths,
# - adds 'darwin' as possible platform-specific subdirectory.
# - avoids fatal error when JDK not found

# ===========================================================================
#    http://www.gnu.org/software/autoconf-archive/ax_jni_include_dir.html
# ===========================================================================
#
# SYNOPSIS
#
#   AX_JNI_INCLUDE_DIR
#
# DESCRIPTION
#
#   AX_JNI_INCLUDE_DIR finds include directories needed for compiling
#   programs using the JNI interface.
#
#   JNI include directories are usually in the Java distribution. This is
#   deduced from the value of $JAVA_HOME, $JAVAC, or the path to "javac", in
#   that order. When this macro completes, a list of directories is left in
#   the variable JNI_INCLUDE_DIRS.
#
#   Example usage follows:
#
#     AX_JNI_INCLUDE_DIR
#
#     for JNI_INCLUDE_DIR in $JNI_INCLUDE_DIRS
#     do
#             CPPFLAGS="$CPPFLAGS -I$JNI_INCLUDE_DIR"
#     done
#
#   If you want to force a specific compiler:
#
#   - at the configure.in level, set JAVAC=yourcompiler before calling
#   AX_JNI_INCLUDE_DIR
#
#   - at the configure level, setenv JAVAC
#
#   Note: This macro can work with the autoconf M4 macros for Java programs.
#   This particular macro is not part of the original set of macros.
#
# LICENSE
#
#   Copyright (c) 2008 Don Anderson <dda@sleepycat.com>
#   Copyright (c) 2014 University of California, San Francisco
#
#   Copying and distribution of this file, with or without modification, are
#   permitted in any medium without royalty provided the copyright notice
#   and this notice are preserved. This file is offered as-is, without any
#   warranty.

# based on: serial 10

AU_ALIAS([AC_JNI_INCLUDE_DIR], [AX_JNI_INCLUDE_DIR])
AC_DEFUN([AX_JNI_INCLUDE_DIR],[

JNI_INCLUDE_DIRS=""

# Fake loop to bail via `break'
while true
do

if test "x$JAVA_HOME" != x; then
	_JTOPDIR="$JAVA_HOME"
	_JINC="$_JTOPDIR/include"
else
	_ACJNI_JAVAC="$JAVAC"
	if test "x$_ACJNI_JAVAC" = x; then
		AC_PATH_PROG([_ACJNI_JAVAC], [javac], [no])
	fi
	if test "x$_ACJNI_JAVAC" = xno; then
		AC_MSG_WARN([cannot find JDK; try setting \$JAVAC or \$JAVA_HOME])
		break
	fi
	_ACJNI_FOLLOW_SYMLINKS("$_ACJNI_JAVAC")
	_JTOPDIR=`echo "$_ACJNI_FOLLOWED" | sed -e 's://*:/:g' -e 's:/[[^/]]*$::'`

	case "$host_os" in
		darwin*)        _JTOPDIR=`echo "$_JTOPDIR" | sed -e 's:/[[^/]]*$::'`
				_JINC="$_JTOPDIR/Headers";;
		*)              _JINC="$_JTOPDIR/include";;
	esac
fi

_AS_ECHO_LOG([_JTOPDIR=$_JTOPDIR])
_AS_ECHO_LOG([_JINC=$_JINC])

# On Mac OS X 10.6.4, jni.h is a symlink:
# /System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers/jni.h
# -> ../../CurrentJDK/Headers/jni.h.
AC_CHECK_FILE([$_JINC/jni.h],
	[JNI_INCLUDE_DIRS="$JNI_INCLUDE_DIRS $_JINC"],
	[_JTOPDIR=`echo "$_JTOPDIR" | sed -e 's:/[[^/]]*$::'`
	 AC_CHECK_FILE([$_JTOPDIR/include/jni.h],
		[JNI_INCLUDE_DIRS="$JNI_INCLUDE_DIRS $_JTOPDIR/include"],
		AC_MSG_WARN([cannot find JDK header files]))
		break
	])

# get the likely subdirectories for system specific java includes
case "$host_os" in
bsdi*)          _JNI_INC_SUBDIRS="bsdos";;
freebsd*)       _JNI_INC_SUBDIRS="freebsd";;
linux*)         _JNI_INC_SUBDIRS="linux genunix";;
osf*)           _JNI_INC_SUBDIRS="alpha";;
solaris*)       _JNI_INC_SUBDIRS="solaris";;
mingw*)		_JNI_INC_SUBDIRS="win32";;
cygwin*)	_JNI_INC_SUBDIRS="win32";;
darwin*)        _JNI_INC_SUBDIRS="darwin";;
*)              _JNI_INC_SUBDIRS="genunix";;
esac

# add any subdirectories that are present
for JINCSUBDIR in $_JNI_INC_SUBDIRS
do
    if test -d "$_JTOPDIR/include/$JINCSUBDIR"; then
         JNI_INCLUDE_DIRS="$JNI_INCLUDE_DIRS $_JTOPDIR/include/$JINCSUBDIR"
    fi
done

break
done # fake loop
])

# _ACJNI_FOLLOW_SYMLINKS <path>
# Follows symbolic links on <path>,
# finally setting variable _ACJNI_FOLLOWED
# ----------------------------------------
AC_DEFUN([_ACJNI_FOLLOW_SYMLINKS],[
# find the include directory relative to the javac executable
_cur="$1"
while ls -ld "$_cur" 2>/dev/null | grep " -> " >/dev/null; do
        AC_MSG_CHECKING([symlink for $_cur])
        _slink=`ls -ld "$_cur" | sed 's/.* -> //'`
        case "$_slink" in
        /*) _cur="$_slink";;
        # 'X' avoids triggering unwanted echo options.
        *) _cur=`echo "X$_cur" | sed -e 's/^X//' -e 's:[[^/]]*$::'`"$_slink";;
        esac
        AC_MSG_RESULT([$_cur])
done
_ACJNI_FOLLOWED="$_cur"
])# _ACJNI
