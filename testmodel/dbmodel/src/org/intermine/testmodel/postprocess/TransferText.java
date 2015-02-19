package org.intermine.testmodel.postprocess;

import java.util.List;
import java.util.ArrayList;

import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.model.testmodel.*;

public class TransferText extends PostProcessor {

    private static final String INDEX_ERROR = "Error getting text for %s at %d - %d";

    public TransferText(ObjectStoreWriter osw) {
        super(osw);
    }

    public void postProcess() throws ObjectStoreException {
        ObjectStoreWriter osw = getObjectStoreWriter();

        QueryClass section = new QueryClass(Section.class);

        Query findSections = new Query();
        findSections.addFrom(section);
        findSections.addToSelect(section);

        // Bad practice pulling all into memory, but the dataset is small.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<Section> sections = new ArrayList<Section>((List) osw.executeSingleton(findSections));

        for (Section sec: sections) {
            if (sec.getText() != null) continue;
            Book book = sec.getBook();
            if (book == null) continue;
            TextLocation loc = sec.getTextLocation();
            if (loc == null) continue;
            Text bookText = book.getText();
            if (bookText == null) continue;
            ClobAccess reference = bookText.getText();
            int start = loc.getRangeStart() - 1;
            int end = Math.min(reference.length(), loc.getRangeEnd());

            Text sectionText = new Text(); 
            sectionText.setLength(end - start);
            sectionText.setLanguage(bookText.getLanguage());
            sectionText.setComposition(sec);
            try {
                sectionText.setText(reference.subSequence(start, end));
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException(String.format(INDEX_ERROR, sec.getName(), start, end), e);
            }
            osw.store(sectionText);

            sec.setText(sectionText);
            osw.store(sec);
        }

    }
}
