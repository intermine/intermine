# vim: set fileencoding=utf8 :

class BibliophileHandler(object):

    def __init__(self, factory):
        self.factory = factory
        self.people = {}
        self.sections = {}

    def get_bibliophile(self, name):
        if name not in self.people:
            self.people[name] = self.factory.add('Bibliophile', name = name)
        return self.people[name]

    def get_section(self, name, span):
        if '-' == span:
            return self.factory.add('Section', name = name)
        else:
            (start, end) = span.split('-')
            if name not in self.sections:
                self.sections[name] = self.factory.add('Section', name = name)
            section = self.sections[name]
            subsection = self.factory.add('Section', name = name + ':' + span)
            section.add_to('subSections', subsection)
            loc = self.factory.add('TextLocation', rangeStart = start, rangeEnd = end)
            subsection.set('textLocation', loc)
            return subsection

    def process(self, lines):
        for line in lines:
            person, section, span, rating, comment = line.strip().split(",", 4)
            bibliophile = self.get_bibliophile(person)
            passage = self.get_section(section, span)
            favourite_passage = self.factory.add('FavouritePassage', comment = comment, rating = float(rating))
            favourite_passage.set('belovedOf', bibliophile)
            favourite_passage.set('passage', passage)
            bibliophile.add_to('favouritePassages', favourite_passage)

