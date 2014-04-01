model = ../../../../intermine/objectstore/model/testmodel/testmodel_model.xml
files = build/verwandlung.xml build/alice-in-wonderland.xml build/sherlock.xml
carmina = build/horace-C-I.xml build/horace-C-II.xml build/horace-C-III.xml

.PHONY: all
all: $(files) $(carmina)

$(files): venv build $(model) ../../data/$(@F)
	. venv/bin/activate; ./process-book $(model) gutenberg ../../data/$(@F) $@

$(carmina): venv build $(model) ../../data/$(@F)
	. venv/bin/activate; ./process-book $(model) perseus ../../data/$(@F) $@

# Build virtual environment and keep in sync with reqs.
venv: venv/bin/activate

venv/bin/activate: requirements.txt
	test -d venv || virtualenv venv
	. venv/bin/activate; pip install --upgrade -r requirements.txt
	touch venv/bin/activate

lib:
	virtualenv .
	source bin/activate
	pip install --upgrade -r requirements.txt

build:
	mkdir build

.PHONY: clean
clean:
	if test -d build; then rm -r build; fi

