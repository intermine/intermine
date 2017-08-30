package org.intermine.objectstore.translating;

import org.intermine.model.testmodel.Bank;
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;

class CompanyTranslator extends Translator
{
    public void setObjectStore(ObjectStore os) {
    }

    public Query translateQuery(Query query) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        return q;
    }

    public Object translateToDbObject(Object o) {
        return o;
    }

    public Object translateFromDbObject(Object o) {
        if (o instanceof Company) {
            Bank bank = new Bank();
            bank.setId(((Company) o).getId());
            bank.setName(((Company) o).getName());
            return bank;
        } else {
            return o;
        }
    }

    public Object translateIdToIdentifier(Integer id) {
        return id;
    }
}
