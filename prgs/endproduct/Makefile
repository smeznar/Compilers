JAVA	= java
JAVAC	= javac
RM	= 'rm' -fr

SRCDIR	= ../../srcs
DSTDIR	= .

SRCS	= $(shell test -d $(SRCDIR) && find $(SRCDIR) -name '*.java')

TARGET	= $(notdir $(shell pwd))
LOGGED	= $(notdir $(shell pwd))

all: distclean tests run_mmix

compiler/Main.class: $(SRCS)
	$(RM) -fr compiler module-info.class
	$(JAVAC) --source-path $(SRCDIR) -d $(DSTDIR) $(SRCDIR)/compiler/Main.java

tests: compiler/Main.class $(shell find . -name '*.prev')
	@echo ; \
	for SRC in `'ls' -1 *.prev` ; do \
		echo "TEST $$SRC:" ; \
		$(JAVA) -classpath $(DSTDIR) compiler.Main $$SRC --target-phase=$(TARGET) --logged-phase=$(LOGGED); \
		echo ; \
	done ; \

test: compiler/Main.class $(SRC)
	@ echo "TEST $$SRC:"
	@ $(JAVA) -classpath $(DSTDIR) compiler.Main $$SRC --target-phase=$(TARGET) --logged-phase=$(LOGGED)
	@ NAME=`basename $$SRC .prev` ; \

.PHONY: distclean

run_mmix:
	@for SRC in `'ls' -1 *.mms` ; do \
		echo "Compiling $$SRC:" ; \
		`./mmixal $$SRC` \
		echo ; \
	done;
	@for SRC in `'ls' -1 *.mmo` ; do \
		echo "Testing $$SRC:" ; \
		./mmix $$SRC > output/`basename $$SRC .mmo`.txt; \
		echo "Done testing $$SRC, output is stored in `basename $$SRC .mmo`.txt"; \
		echo ; \
	done;

distclean:
	$(RM) *~ *.mmo *.txt
	$(RM) -fr compiler module-info.class
	find . -type d -depth 1 -exec $(MAKE) -C {} distclean \;
