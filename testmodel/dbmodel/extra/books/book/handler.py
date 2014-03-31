# vim: set fileencoding=utf8 :

from xml.sax.handler import ContentHandler
from collections import defaultdict

class CharacterContext:
    PROSE, VERSE, NONE = range(3)

class BookHandler(ContentHandler):

    def __init__(self, factory):
        ContentHandler.__init__(self)
        self.factory = factory
        self.text_position = 1
        self.poems = {}
        self.character_context = None
        self.current = {}
        self.untitled_poems = 0
        self.text_buffer = []
        self.locations = []
        self.current_chunk = []
        self.debug = False

    def startDocument(self):
        pass

    def endDocument(self):
        pass

    def startElement(self, name, attrs):
        handler = getattr(self, 'start_' + name)
        self.current[name] = handler(attrs)

    def endElement(self, name):
        handler = getattr(self, 'end_' + name)
        handler()

    def endCurrentComposition(self, name):
        self.endChunk()
        comp = self.current.pop(name)
        loc = comp.get('textLocation')
        loc.set('end', self.text_position)
        if name != 'book' and self.debug:
            text = ' '.join(self.text_buffer)
            start = loc.get('start')
            end = loc.get('end')
            print u'{}: {}-{}\n\t{}'.format(name, start, end, text[start - 1:end])

        self.character_context = CharacterContext.NONE

    def start_book(self, attrs):
        fac = self.factory
        author = fac.add('Author', name = attrs.get('Author'))
        book = fac.add('Book', author = author)
        book.set('title', attrs.get('Title'))
        book.set('name', attrs.get('Title'))
        book.set('publicationDate', attrs.get('Publication-Date'))
        text = fac.add('Text', composition = book, language = attrs.get('Language'))
        book.set('text', text)
        self.untitled_poems = 0
        self.text_position = 1
        self.text_buffer = []
        book_location = fac.add('TextLocation', start = self.text_position)
        book_location.set('foundIn', book)
        book.set('textLocation', book_location)
        return book

    def end_book(self):
        self.poems.clear()
        book = self.current['book']
        self.endCurrentComposition('book')
        book_text = ' '.join(self.text_buffer)
        text = book.get('text')
        text.set('length', len(book_text))
        text.set('text', book_text)

    def initLocation(self, composition):
        location = self.factory.add('TextLocation')
        location.set('start', self.text_position)
        location.set('foundIn', composition)
        composition.set('textLocation', location)

    def start_chapter(self, attrs):
        chapter = self.factory.add('Chapter')
        book = self.current['book']
        chapter.set('title', attrs.get('Title'))
        chapter.set('name', "{}-{}".format(
            book.get('title'), attrs.get('Title')))
        chapter.set('book', book)
        book.add_to('subSections', chapter)
        self.initLocation(chapter)
        return chapter

    def end_chapter(self):
        self.endCurrentComposition('chapter')

    def start_paragraph(self, attrs):
        fac = self.factory
        book = self.current['book']
        chapter = self.current['chapter']
        paragraph = fac.add('Paragraph', book = book, chapter = chapter)
        chapter.add_to('paragraphs', paragraph)
        n = len(chapter.get('paragraphs'))
        paragraph.set('number', n)
        paragraph.set('name', u'{}-{}-¶{}'.format(
            book.get('title'), chapter.get('title'), n))
        self.character_context = CharacterContext.PROSE
        chapter.add_to('subSections', paragraph)
        self.initLocation(paragraph)
        return paragraph

    def end_paragraph(self):
        self.endCurrentComposition('paragraph')

    def start_poem(self, attrs):
        fac = self.factory
        book = self.current['book']
        chapter = self.current['chapter']
        title = attrs.get('name', 'Untitled')
        if title == 'Untitled':
            self.untitled_poems += 1
            name = '{}:Untitled({})'.format(book.get('title'), self.untitled_poems)
        else:
            name = '{}:{}'.format(book.get('name'), title)

        if name in self.poems:
            poem = self.poems[name]
        else:
            poem = fac.add('Poem',
                book = book, name = name, title = title)
            chapter.add_to('subSections', poem)
        self.poems[name] = poem
        self.character_context = CharacterContext.VERSE
        self.initLocation(poem)
        return poem

    def end_poem(self):
        self.endCurrentComposition('poem')

    def start_stanza(self, attrs = None, class_name = 'Stanza'):
        fac = self.factory
        book = self.current['book']
        poem = self.current['poem']
        stanza = fac.add(class_name, book = book, poem = poem)
        poem.add_to('stanzas', stanza)
        stanza.set('name', '{}:{}-{}'.format(
            book.get('title'), poem.get('name'), len(poem.get('stanzas'))))
        poem.add_to('subSections', stanza)
        self.character_context = CharacterContext.VERSE
        self.initLocation(stanza)
        return stanza

    def end_stanza(self):
        self.endCurrentComposition('stanza')

    def start_chorus(self, attrs):
        chorus = self.start_stanza(attrs, 'Chorus')
        self.current['stanza'] = chorus
        return chorus

    def end_chorus(self):
        self.end_stanza()

    def characters(self, content):
        self.current_chunk.extend(content)

    def createLines(self, chunk_start, lines):
        book = self.current['book']
        if 'stanza' in self.current:
            stanza = self.current['stanza']
        else:
            stanza = self.start_stanza()
        poem = stanza.get('poem')
        line_start = chunk_start
        offset = len([l for s in poem.get('stanzas') for l in s.get('lines')])
        for idx, line_text in enumerate(lines):
            line_no = offset + 1 + idx
            name = '{};{}'.format(poem.get('name'), line_no)
            text_location = self.factory.add('TextLocation')
            text_location.set('start', line_start)
            text_location.set('end', line_start + len(line_text))
            line = self.factory.add('Line',
                number = line_no, name = name,
                book = book, stanza = stanza,
                textLocation = text_location)
            text_location.set('foundIn', line)
            line_start += len(line_text) + 1
            stanza.add_to('lines', line)
            stanza.get('textLocation').set('end', text_location.get('end'))
            if self.debug:
                text = ' '.join(self.text_buffer)
                start = text_location.get('start')
                end = text_location.get('end')
                print u'{}: {}-{}\n\t{}'.format('line', start, end, text[start - 1:end])

    def endChunk(self):
        chunk = ''.join(self.current_chunk)
        self.current_chunk = []
        lines = filter(len, map(lambda line: line.strip(), chunk.split('\n')))
        if not len(lines):
            return
        if len(self.text_buffer):
            self.text_position += 1
        chunk_length = sum(map(len, lines)) + len(lines) - 1
        self.text_buffer.extend(lines)
        if self.character_context is CharacterContext.VERSE:
            self.createLines(self.text_position, lines)
        self.text_position += chunk_length

