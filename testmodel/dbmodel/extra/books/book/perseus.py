# vim: set fileencoding=utf8 :

from xml.sax.handler import ContentHandler

IGNORABLE = set(['text', 'body'])

class BookHandler(ContentHandler):

    def __init__(self, factory):
        ContentHandler.__init__(self)
        self.factory = factory
        self.text_position = 1
        self.character_context = None
        self.division_map = {}
        self.text_buffer = []
        self.current_line = []
        self.current_chunk = []
        self.debug = False
        self.book = None
        self.stanza = None
        self.poem = None
        self.line = None

    def startDocument(self):
        pass

    def endDocument(self):
        pass

    def startElement(self, name, attrs):
        if name.startswith('TEI') or name in IGNORABLE:
            return

        if name.startswith('div'):
            self.start_division(name, attrs)
        elif name == 'lg':
            self.start_stanza(attrs)
        elif name == 'l':
            self.start_line(attrs)
        else:
            raise ValueError("Unknown tag: " + name)

    def start_division(self, name, attrs):
        div_type = attrs.get('type')
        if div_type == 'Book':
            self.start_book(attrs)
            self.division_map[name] = 'Book'
        elif div_type == 'Poem':
            self.start_poem(attrs)
            self.division_map[name] = 'Poem'
        else:
            raise ValueError("Unknown division type: " + div_type)

    def endElement(self, name):
        if name.startswith('TEI') or name in IGNORABLE:
            return

        if name.startswith('div'):
            self.end_division(name)
        elif name == 'lg':
            self.end_stanza()
        elif name == 'l':
            self.end_line()
        else:
            raise ValueError("Unknown tag: " + name)

    def end_division(self, name):
        div_type = self.division_map.pop(name)
        if div_type == 'Book':
            self.end_book()
        elif div_type == 'Poem':
            self.end_poem()
        else:
            raise ValueError("Unknown division type: " + div_type)

    def set_text_end(self, comp):
        loc = comp.get('textLocation')
        loc.set('end', self.text_position)

    def start_book(self, attrs):
        fac = self.factory
        author = fac.add('Author', name = attrs.get('author'))
        book = fac.add('Book', author = author)
        book.set('title', attrs.get('title'))
        book.set('name', '{} {}'.format(attrs.get('title'), attrs.get('n')))
        book.set('identifier', attrs.get('identifier'))
        text = fac.add('Text',
                composition = book, language = attrs.get('language'))
        book.set('text', text)
        self.text_position = 1
        self.text_buffer = []
        self.initLocation(book)
        self.book = book

    def end_book(self):
        book = self.book
        self.set_text_end(book)
        book_text = ' '.join(self.text_buffer)
        text = book.get('text')
        text.set('length', len(book_text))
        text.set('text', book_text)
        self.book = None

    def initLocation(self, composition):
        location = self.factory.add('TextLocation')
        location.set('start', self.text_position)
        location.set('foundIn', composition)
        composition.set('textLocation', location)

    def create_subsection(self, cls):
        book = self.book
        author = book.get('author')
        comp = self.factory.add(cls, book = book, author = author)
        self.initLocation(comp)
        return comp

    def start_poem(self, attrs):
        fac = self.factory

        poem = self.create_subsection('Poem')
        self.book.add_to('subSections', poem)
        poem.set('name', u'{}-{}'.format(
            self.book.get('name'),
            len(self.book.get('subSections'))))

        self.poem = poem

    def end_poem(self):
        if self.stanza is not None: self.end_stanza()
        self.set_text_end(self.poem)
        self.poem = None

    def start_stanza(self, attrs = None):
        stanza = self.create_subsection('Stanza')
        book = stanza.get('book')
        poem = self.poem
        stanza.set('poem', poem)
        poem.add_to('stanzas', stanza)
        poem.add_to('subSections', stanza)
        n = len(poem.get('stanzas'))
        stanza.set('name', '{},{}'.format(poem.get('name'), n))
        self.stanza = stanza
        return stanza

    def end_stanza(self):
        self.set_text_end(self.stanza)
        self.stanza = None

    def start_line(self, attrs):
        line = self.create_subsection('Line')
        stanza = self.stanza if self.stanza else self.start_stanza()
        stanza.add_to('lines', line)
        stanza.add_to('subSections', line)
        n = len(stanza.get('lines'))
        line.set('stanza', stanza)
        line.set('name', u'{},{}'.format(stanza.get('name'), n))
        line.set('number', n)
        self.current_chunk = []
        self.line = line

    def characters(self, content):
        self.current_chunk.extend(content)

    def end_line(self):
        line = ''.join(self.current_chunk)
        if len(self.text_buffer): self.text_position += 1
        if not self.poem.get('title'): self.poem.set('title', line)

        self.text_buffer.append(line)
        self.text_position += len(line)
        self.set_text_end(self.line)
        self.line = None
