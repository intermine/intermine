ifdef INTERBASE_COORDS
BIOSEG := bioseg0
PG_CPPFLAGS = -I. -DINTERBASE_COORDS
else
BIOSEG := bioseg
PG_CPPFLAGS = -I.
endif

MODULE_big = $(BIOSEG)
OBJS = bioseg.o
DATA_built = bioseg.sql
DATA = uninstall_bioseg.sql
REGRESS = $(BIOSEG)



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
