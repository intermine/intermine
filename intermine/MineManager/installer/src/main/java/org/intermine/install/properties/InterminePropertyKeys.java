package org.intermine.install.properties;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * This interface simply defines the property keys from a mine's user
 * configuration file that the installer application understands and
 * works with (plus a couple of extras).
 */
public interface InterminePropertyKeys {

    String INTERMINE_HOME = "intermineHome";
    String MINE_NAME = "mineName";
    String MINE_HOME = "mineHome";
    
    String PRODUCTION_SERVER = "db.production.datasource.serverName";
    String PRODUCTION_NAME = "db.production.datasource.databaseName";
    String PRODUCTION_USER_NAME = "db.production.datasource.user";
    String PRODUCTION_PASSWORD = "db.production.datasource.password";
    String PRODUCTION_ENCODING = "db.production.datasource.encoding";
    
    String ITEMS_SERVER = "db.common-tgt-items.datasource.serverName";
    String ITEMS_NAME = "db.common-tgt-items.datasource.databaseName";
    String ITEMS_USER_NAME = "db.common-tgt-items.datasource.user";
    String ITEMS_PASSWORD = "db.common-tgt-items.datasource.password";
    String ITEMS_ENCODING = "db.common-tgt-items.datasource.encoding";
    
    String PROFILE_SERVER = "db.userprofile-production.datasource.serverName";
    String PROFILE_NAME = "db.userprofile-production.datasource.databaseName";
    String PROFILE_USER_NAME = "db.userprofile-production.datasource.user";
    String PROFILE_PASSWORD = "db.userprofile-production.datasource.password";
    String PROFILE_ENCODING = "db.userprofile-production.datasource.encoding";
    
    String WEBAPP_DEPLOY_URL = "webapp.deploy.url";
    String WEBAPP_PATH = "webapp.path";
    String WEBAPP_MANAGER = "webapp.manager";
    String WEBAPP_PASSWORD = "webapp.password";
    String WEBAPP_BASE_URL = "webapp.baseurl";
    
    String SUPERUSER_ACCOUNT = "superuser.account";
    String SUPERUSER_PASSWORD = "superuser.initialPassword";
    String MAIL_HOST = "mail.host";
    String MAIL_FROM = "mail.from";
    String MAIL_SUBJECT = "mail.subject";
    String MAIL_TEXT = "mail.text";
    
    String PROJECT_TITLE = "project.title";
    String PROJECT_SUBTITLE = "project.subTitle";
    String PROJECT_VERSION = "project.releaseVersion";
    String PROJECT_PREFIX = "project.sitePrefix";
    String PROJECT_HELP = "project.helpLocation";
    String FEEDBACK = "feedback.destination";
    
    String VERBOSE_LOG = "os.production.verboseQueryLog";
    String STANDALONE_PROJECT = "project.standalone";
}
