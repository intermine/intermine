
-----------------------------------------------------------------------------
-- UserProfile
-----------------------------------------------------------------------------
DROP TABLE UserProfile CASCADE;


CREATE TABLE UserProfile
(
    username TEXT,
    id INTEGER NOT NULL,
    password TEXT,
    CONSTRAINT UserProfile_pkey UNIQUE (id)
);


-----------------------------------------------------------------------------
-- SavedBag
-----------------------------------------------------------------------------
DROP TABLE SavedBag CASCADE;


CREATE TABLE SavedBag
(
    id INTEGER NOT NULL,
    bag TEXT,
    userProfileId INTEGER,
    CONSTRAINT SavedBag_pkey UNIQUE (id)
);


-----------------------------------------------------------------------------
-- SavedQuery
-----------------------------------------------------------------------------
DROP TABLE SavedQuery CASCADE;


CREATE TABLE SavedQuery
(
    id INTEGER NOT NULL,
    query TEXT,
    userProfileId INTEGER,
    CONSTRAINT SavedQuery_pkey UNIQUE (id)
);


-----------------------------------------------------------------------------
-- SavedTemplateQuery
-----------------------------------------------------------------------------
DROP TABLE SavedTemplateQuery CASCADE;


CREATE TABLE SavedTemplateQuery
(
    templateQuery TEXT,
    id INTEGER NOT NULL,
    userProfileId INTEGER,
    CONSTRAINT SavedTemplateQuery_pkey UNIQUE (id)
);


-----------------------------------------------------------------------------
-- Tag
-----------------------------------------------------------------------------
DROP TABLE Tag CASCADE;


CREATE TABLE Tag
(
    id INTEGER NOT NULL,
    objectIdentifier TEXT,
    tagName TEXT,
    type TEXT,
    userProfileId INTEGER,
    CONSTRAINT Tag_pkey UNIQUE (id)
);


-----------------------------------------------------------------------------
-- InterMineObject
-----------------------------------------------------------------------------
DROP TABLE InterMineObject CASCADE;


CREATE TABLE InterMineObject
(
    OBJECT TEXT,
    id INTEGER NOT NULL,
    CONSTRAINT InterMineObject_pkey UNIQUE (id)
);


-----------------------------------------------------------------------------
-- intermine_metadata
-----------------------------------------------------------------------------
DROP TABLE intermine_metadata CASCADE;


CREATE TABLE intermine_metadata
(
    key TEXT,
    value TEXT,
    CONSTRAINT intermine_metadata_key UNIQUE (key)
);


----------------------------------------------------------------------
-- intermine_metadata                                                      
----------------------------------------------------------------------


----------------------------------------------------------------------
-- UserProfile                                                      
----------------------------------------------------------------------


----------------------------------------------------------------------
-- SavedBag                                                      
----------------------------------------------------------------------


----------------------------------------------------------------------
-- SavedQuery                                                      
----------------------------------------------------------------------


----------------------------------------------------------------------
-- SavedTemplateQuery                                                      
----------------------------------------------------------------------


----------------------------------------------------------------------
-- Tag                                                      
----------------------------------------------------------------------


----------------------------------------------------------------------
-- InterMineObject                                                      
----------------------------------------------------------------------

