ifdef INTERBASE_COORDS
BIOSEG := bioseg0
else
BIOSEG := bioseg
endif

MODULE_big = $(BIOSEG)
OBJS = bioseg.o
DATA_built = bioseg.sql
DATA = uninstall_bioseg.sql
REGRESS = $(BIOSEG)

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

%.sql: %.sql.in
	sed 's,MODULE_PATHNAME,$$libdir/$*,g' $< | sed 's,$$BIOSEG_TYPE,$(BIOSEG),g' >$@
