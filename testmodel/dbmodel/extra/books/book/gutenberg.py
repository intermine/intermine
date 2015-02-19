# vim: set fileencoding=utf8 :

from xml.sax.handler import ContentHandler

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
        loc.set('rangeEnd', self.text_position)
        self.character_context = CharacterContext.NONE

    def start_book(self, attrs):
        fac = self.factory
        author = fac.add('Author', name = attrs.get('Author'))
        self.current['author'] = author
        book = fac.add('Book', author = author)
        book.set('title', attrs.get('Title'))
        book.set('name', attrs.get('Title'))
        book.set('identifier', attrs.get('Aleph', attrs.get('Identifier')))
        book.set('publicationDate', attrs.get('Publication-Date'))
        text = fac.add('Text', composition = book, language = attrs.get('Language'))
        book.set('text', text)
        self.untitled_poems = 0
        self.text_position = 1
        self.text_buffer = []
        book_location = fac.add('TextLocation', rangeStart = self.text_position)
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
        location.set('rangeStart', self.text_position)
        location.set('foundIn', composition)
        composition.set('textLocation', location)

    def create_subsection(self, cls):
        book = self.current['book']
        author = self.current['author']
        comp = self.factory.add(cls, book = book, author = author)
        self.initLocation(comp)
        return comp

    def start_chapter(self, attrs, cls = 'Chapter'):
        chapter = self.create_subsection(cls)
        book = chapter.get('book')
        chapter.set('title', attrs.get('Title'))
        book.add_to('chapters', chapter)
        num = len(book.get('chapters'))
        chapter.set('name', u'{}-ch{}'.format(book.get('identifier'), num))
        book.add_to('subSections', chapter)
        return chapter

    def end_chapter(self):
        self.endCurrentComposition('chapter')

    def start_story(self, attrs):
        story = self.start_chapter(attrs, 'Story')
        book = story.get('book')
        story.set('name', u'{}-{}'.format(book.get('name'), attrs.get('Number')))
        self.current['chapter'] = story
        return story

    def end_story(self):
        self.end_chapter()
        self.current.pop('story')

    def start_section(self, attrs):
        section = self.create_subsection('Section')
        chapter = self.current['chapter']
        if chapter:
            chapter.add_to('subSections', section)
        return section

    def end_section(self):
        self.endCurrentComposition('section')

    def start_paragraph(self, attrs):
        paragraph = self.create_subsection('Paragraph')
        book = paragraph.get('book')
        chapter = self.current['chapter']
        paragraph.set('chapter', chapter)
        chapter.add_to('paragraphs', paragraph)
        n = len(chapter.get('paragraphs'))
        paragraph.set('number', n)
        paragraph.set('name', u'{}-¶{}'.format(chapter.get('name'), n))
        self.character_context = CharacterContext.PROSE
        section = self.current.get('section')
        if section:
            section.add_to('subSections', paragraph)
        else:
            chapter.add_to('subSections', paragraph)

        return paragraph

    def end_paragraph(self):
        self.endCurrentComposition('paragraph')

    def start_poem(self, attrs):
        fac = self.factory
        book = self.current['book']
        chapter = self.current['chapter']
        title = attrs.get('name')

        if title:
            name = '{}:poem-{}'.format(book.get('identifier'), title)
        else:
            name = None

        if name and name in self.poems:
            poem = self.poems[name]
        else:
            poem = self.create_subsection('Poem')
            if name: poem.set('name', name)
            if title: poem.set('title', title)
            chapter.add_to('subSections', poem)

        if name: self.poems[name] = poem

        self.character_context = CharacterContext.VERSE
        return poem

    def end_poem(self):
        self.endCurrentComposition('poem')

    def start_stanza(self, attrs = None, class_name = 'Stanza'):
        stanza = self.create_subsection(class_name)
        book = stanza.get('book')
        poem = self.current['poem']
        stanza.set('poem', poem)
        poem.add_to('stanzas', stanza)
        n = len(poem.get('stanzas'))
        stanza.set('name', '{},{}'.format(poem.get('name'), n))
        poem.add_to('subSections', stanza)
        self.character_context = CharacterContext.VERSE
        return stanza

    def end_stanza(self):
        self.endCurrentComposition('stanza')

    def start_chorus(self, attrs):
        chorus = self.start_stanza(attrs, 'Chorus')
        self.current['stanza'] = chorus
        return chorus

    def end_chorus(self):
        self.end_stanza()
        self.current.pop('chorus')

    def characters(self, content):
        self.current_chunk.extend(content)

    def createLines(self, chunk_start, lines):
        book = self.current['book']
        if 'stanza' in self.current:
            stanza = self.current['stanza']
        else:
            stanza = self.start_stanza()
        poem = stanza.get('poem')
        if not poem.get('title'):
            line_0 = lines[0].strip(',.')
            poem.set('title', line_0)
            poem.set('name', u'{}:poem-{}'.format(book.get('identifier'), line_0))
            stanza.set('name', u'{},{}'.format(poem.get('name'), len(poem.get('stanzas'))))
            self.poems[poem.get('name')] = poem

        line_start = chunk_start
        offset = len([l for s in poem.get('stanzas') for l in s.get('lines')])
        author = self.current['author']
        for idx, line_text in enumerate(lines):
            line_no = offset + 1 + idx
            name = '{};{}'.format(poem.get('name'), line_no)
            text_location = self.factory.add('TextLocation')
            text_location.set('rangeStart', line_start)
            text_location.set('rangeEnd', line_start + len(line_text))
            line = self.factory.add('Line',
                number = line_no, name = name,
                author = author, book = book, stanza = stanza,
                textLocation = text_location)
            text_location.set('foundIn', line)
            line_start += len(line_text) + 1
            stanza.add_to('lines', line)
            stanza.add_to('subSections', line)
            stanza.get('textLocation').set('rangeEnd', text_location.get('rangeEnd'))
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

