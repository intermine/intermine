model = ../../../../intermine/objectstore/model/testmodel/testmodel_model.xml
files = build/verwandlung.xml build/alice-in-wonderland.xml

.PHONY: all
all: $(files)

$(files): lib build $(model) ../../data/$(@F)
	./process-book $(model) ../../data/$(@F) > $@

lib:
	virtualenv .
	source bin/activate
	pip install --upgrade -r requirements.txt

build:
	mkdir build

.PHONY: clean
clean:
	rm -r build

