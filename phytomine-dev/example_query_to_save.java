        /*Query q = new Query();
        QueryClass qc = new QueryClass(DataSource.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryValue qv = new QueryValue(dbName);
        QueryField qf = new QueryField(qc,"name");
        SimpleConstraint sc = new SimpleConstraint(qf,ConstraintOp.EQUALS, qv);
        q.setConstraint(sc);
        Set returnList = new HashSet();
        ObjectStore os;
        DataSource dataSource = null;
        try {
        	os = ObjectStoreFactory.getObjectStore("os.production");
        } catch (Exception e) {
        	throw new ObjectStoreException(e);
        }
        try {
        	Results res = os.execute(q);
        	Iterator resIter = res.iterator();
            while (resIter.hasNext()) {
                ResultsRow rr = (ResultsRow) resIter.next();
                returnList.add(rr.get(0));
            } 
        }catch (Exception e) {
        }
