MODULE_big = bioseg
OBJS = bioseg.o
DATA_built = bioseg.sql
DATA = uninstall_bioseg.sql
REGRESS = bioseg

PG_CPPFLAGS = -I.

ifdef USE_PGXS
PGXS := $(shell pg_config --pgxs)
include $(PGXS)
else
subdir = contrib/bioseg
top_builddir = ../..
include $(top_builddir)/src/Makefile.global
include $(top_srcdir)/contrib/contrib-global.mk
endif
